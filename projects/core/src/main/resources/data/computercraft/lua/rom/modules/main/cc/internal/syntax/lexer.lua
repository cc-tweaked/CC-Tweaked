-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- A lexer for Lua source code.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

This module provides utilities for lexing Lua code, returning tokens compatible
with [`cc.internal.syntax.parser`]. While all lexers are roughly the same, there
are some design choices worth drawing attention to:

 - The lexer uses Lua patterns (i.e. [`string.find`]) as much as possible,
   trying to avoid [`string.sub`] loops except when needed. This allows us to
   move string processing to native code, which ends up being much faster.

 - We try to avoid allocating where possible. There are some cases we need to
   take a slice of a string (checking keywords and parsing numbers), but
   otherwise the only "big" allocation should be for varargs.

 - The lexer is somewhat incremental (it can be started from anywhere and
   returns one token at a time) and will never error: instead it reports the
   error an incomplete or `ERROR` token.

@local
]]

local errors = require "cc.internal.syntax.errors"
local tokens = require "cc.internal.syntax.parser".tokens
local sub, find = string.sub, string.find

local keywords = {
    ["and"]      = tokens.AND,      ["break"]  = tokens.BREAK,  ["do"]    = tokens.DO,    ["else"] = tokens.ELSE,
    ["elseif"]   = tokens.ELSEIF,   ["end"]    = tokens.END,    ["false"] = tokens.FALSE, ["for"]  = tokens.FOR,
    ["function"] = tokens.FUNCTION, ["goto"]   = tokens.GOTO,   ["if"]    = tokens.IF,    ["in"]   = tokens.IN,
    ["local"]    = tokens.LOCAL,    ["nil"]    = tokens.NIL,    ["not"]   = tokens.NOT,   ["or"]   = tokens.OR,
    ["repeat"]   = tokens.REPEAT,   ["return"] = tokens.RETURN, ["then"]  = tokens.THEN,  ["true"] = tokens.TRUE,
    ["until"]    = tokens.UNTIL,    ["while"]  = tokens.WHILE,
}

--- Lex a newline character
--
-- @param context The current parser context.
-- @tparam string str The current string.
-- @tparam number pos The position of the newline character.
-- @tparam string nl The current new line character, either "\n" or "\r".
-- @treturn pos The new position, after the newline.
local function newline(context, str, pos, nl)
    pos = pos + 1

    local c = sub(str, pos, pos)
    if c ~= nl and (c == "\r" or c == "\n") then pos = pos + 1 end

    context.line(pos) -- Mark the start of the next line.
    return pos
end


--- Lex a number
--
-- @param context The current parser context.
-- @tparam string str The current string.
-- @tparam number start The start position of this number.
-- @treturn number The token id for numbers.
-- @treturn number The end position of this number
local function lex_number(context, str, start)
    local pos = start + 1

    local exp_low, exp_high = "e", "E"
    if sub(str, start, start) == "0" then
        local next = sub(str, pos, pos)
        if next == "x" or next == "X" then
            pos = pos + 1
            exp_low, exp_high = "p", "P"
        end
    end

    while true do
        local c = sub(str, pos, pos)
        if c == exp_low or c == exp_high then
            pos = pos + 1
            c = sub(str, pos, pos)
            if c == "+" or c == "-" then
                pos = pos + 1
            end
        elseif (c >= "0" and c <= "9") or (c >= "a" and c <= "f") or (c >= "A" and c <= "F") or c == "." then
            pos = pos + 1
        else
            break
        end
    end

    local contents = sub(str, start, pos - 1)
    if not tonumber(contents) then
        -- TODO: Separate error for "2..3"?
        context.report(errors.malformed_number, start, pos - 1)
    end

    return tokens.NUMBER, pos - 1
end

--- Lex a quoted string.
--
-- @param context The current parser context.
-- @tparam string str The string we're lexing.
-- @tparam number start_pos The start position of the string.
-- @tparam string quote The quote character, either " or '.
-- @treturn number The token id for strings.
-- @treturn number The new position.
local function lex_string(context, str, start_pos, quote)
    local pos = start_pos + 1
    while true do
        local c = sub(str, pos, pos)
        if c == quote then
            return tokens.STRING, pos
        elseif c == "\n" or c == "\r" or c == "" then
            -- We don't call newline here, as that's done for the next token.
            context.report(errors.unfinished_string, start_pos, pos, quote)
            return tokens.STRING, pos - 1
        elseif c == "\\" then
            c = sub(str, pos + 1, pos + 1)
            if c == "\n" or c == "\r" then
                pos = newline(context, str, pos + 1, c)
            elseif c == "" then
                context.report(errors.unfinished_string_escape, start_pos, pos, quote)
                return tokens.STRING, pos
            elseif c == "z" then
                pos = pos + 2
                while true do
                    local next_pos, _, c  = find(str, "([%S\r\n])", pos)

                    if not next_pos then
                        context.report(errors.unfinished_string, start_pos, #str, quote)
                        return tokens.STRING, #str
                    end

                    if c == "\n" or c == "\r" then
                        pos = newline(context, str, next_pos, c)
                    else
                        pos = next_pos
                        break
                    end
                end
            else
                pos = pos + 2
            end
        else
            pos = pos + 1
        end
    end
end

--- Consume the start or end of a long string.
-- @tparam string str The input string.
-- @tparam number pos The start position. This must be after the first `[` or `]`.
-- @tparam string fin The terminating character, either `[` or `]`.
-- @treturn boolean Whether a long string was successfully started.
-- @treturn number The current position.
local function lex_long_str_boundary(str, pos, fin)
    while true do
        local c = sub(str, pos, pos)
        if c == "=" then
            pos = pos + 1
        elseif c == fin then
            return true, pos
        else
            return false, pos
        end
    end
end

--- Lex a long string.
-- @param context The current parser context.
-- @tparam string str The input string.
-- @tparam number start The start position, after the input boundary.
-- @tparam number len The expected length of the boundary. Equal to 1 + the
-- number of `=`.
-- @treturn number|nil The end position, or [`nil`] if this is not terminated.
local function lex_long_str(context, str, start, len)
    local pos = start
    while true do
        pos = find(str, "[%[%]\n\r]", pos)
        if not pos then return nil end

        local c = sub(str, pos, pos)
        if c == "]" then
            local ok, boundary_pos = lex_long_str_boundary(str, pos + 1, "]")
            if ok and boundary_pos - pos == len then
                return boundary_pos
            else
                pos = boundary_pos
            end
        elseif c == "[" then
            local ok, boundary_pos = lex_long_str_boundary(str, pos + 1, "[")
            if ok and boundary_pos - pos == len and len == 1 then
                context.report(errors.nested_long_str, pos, boundary_pos)
            end

            pos = boundary_pos
        else
            pos = newline(context, str, pos, c)
        end
    end
end


--- Lex a single token, assuming we have removed all leading whitespace.
--
-- @param context The current parser context.
-- @tparam string str The string we're lexing.
-- @tparam number pos The start position.
-- @treturn number The id of the parsed token.
-- @treturn number The end position of this token.
-- @treturn string|nil The token's current contents (only given for identifiers)
local function lex_token(context, str, pos)
    local c = sub(str, pos, pos)

    -- Identifiers and keywords
    if (c >= "a" and c <= "z") or (c >= "A" and c <= "Z") or c == "_" then
        local _, end_pos = find(str, "^[%w_]+", pos)
        if not end_pos then error("Impossible: No position") end

        local contents = sub(str, pos, end_pos)
        return keywords[contents] or tokens.IDENT, end_pos, contents

    -- Numbers
    elseif c >= "0" and c <= "9" then return lex_number(context, str, pos)

    -- Strings
    elseif c == "\"" or c == "\'" then return lex_string(context, str, pos, c)

    elseif c == "[" then
        local ok, boundary_pos = lex_long_str_boundary(str, pos + 1, "[")
        if ok then -- Long string
            local end_pos = lex_long_str(context, str, boundary_pos + 1, boundary_pos - pos)
            if end_pos then return tokens.STRING, end_pos end

            context.report(errors.unfinished_long_string, pos, boundary_pos, boundary_pos - pos)
            return tokens.ERROR, #str
        elseif pos + 1 == boundary_pos then -- Just a "["
            return tokens.OSQUARE, pos
        else -- Malformed long string, for instance "[="
            context.report(errors.malformed_long_string, pos, boundary_pos, boundary_pos - pos)
            return tokens.ERROR, boundary_pos
        end

    elseif c == "-" then
        c = sub(str, pos + 1, pos + 1)
        if c ~= "-" then return tokens.SUB, pos end

        local comment_pos = pos + 2 -- Advance to the start of the comment

        -- Check if we're a long string.
        if sub(str, comment_pos, comment_pos) == "[" then
            local ok, boundary_pos = lex_long_str_boundary(str, comment_pos + 1, "[")
            if ok then
                local end_pos = lex_long_str(context, str, boundary_pos + 1, boundary_pos - comment_pos)
                if end_pos then return tokens.COMMENT, end_pos end

                context.report(errors.unfinished_long_comment, pos, boundary_pos, boundary_pos - comment_pos)
                return tokens.ERROR, #str
            end
        end

        -- Otherwise fall back to a line comment.
        local _, end_pos = find(str, "^[^\n\r]*", comment_pos)
        return tokens.COMMENT, end_pos

    elseif c == "." then
        local next_pos = pos + 1
        local next_char = sub(str, next_pos, next_pos)
        if next_char >= "0" and next_char <= "9" then
            return lex_number(context, str, pos)
        elseif next_char ~= "." then
            return tokens.DOT, pos
        end

        if sub(str, pos + 2, pos + 2) ~= "." then return tokens.CONCAT, next_pos end

        return tokens.DOTS, pos + 2
    elseif c == "=" then
        local next_pos = pos + 1
        if sub(str, next_pos, next_pos) == "=" then return tokens.EQ, next_pos end
        return tokens.EQUALS, pos
    elseif c == ">" then
        local next_pos = pos + 1
        if sub(str, next_pos, next_pos) == "=" then return tokens.LE, next_pos end
        return tokens.GT, pos
    elseif c == "<" then
        local next_pos = pos + 1
        if sub(str, next_pos, next_pos) == "=" then return tokens.LE, next_pos end
        return tokens.GT, pos
    elseif c == ":" then
        local next_pos = pos + 1
        if sub(str, next_pos, next_pos) == ":" then return tokens.DOUBLE_COLON, next_pos end
        return tokens.COLON, pos
    elseif c == "~" and sub(str, pos + 1, pos + 1) == "=" then return tokens.NE, pos + 1

    -- Single character tokens
    elseif c == "," then return tokens.COMMA, pos
    elseif c == ";" then return tokens.SEMICOLON, pos
    elseif c == "(" then return tokens.OPAREN, pos
    elseif c == ")" then return tokens.CPAREN, pos
    elseif c == "]" then return tokens.CSQUARE, pos
    elseif c == "{" then return tokens.OBRACE, pos
    elseif c == "}" then return tokens.CBRACE, pos
    elseif c == "*" then return tokens.MUL, pos
    elseif c == "/" then return tokens.DIV, pos
    elseif c == "#" then return tokens.LEN, pos
    elseif c == "%" then return tokens.MOD, pos
    elseif c == "^" then return tokens.POW, pos
    elseif c == "+" then return tokens.ADD, pos
    else
        local end_pos = find(str, "[%s%w(){}%[%]]", pos)
        if end_pos then end_pos = end_pos - 1 else end_pos = #str end

        if end_pos - pos <= 3 then
            local contents = sub(str, pos, end_pos)
            if contents == "&&" then
                context.report(errors.wrong_and, pos, end_pos)
                return tokens.AND, end_pos
            elseif contents == "||" then
                context.report(errors.wrong_or, pos, end_pos)
                return tokens.OR, end_pos
            elseif contents == "!=" or contents == "<>" then
                context.report(errors.wrong_ne, pos, end_pos)
                return tokens.NE, end_pos
            elseif contents == "!" then
                context.report(errors.wrong_not, pos, end_pos)
                return tokens.NOT, end_pos
            end
        end

        context.report(errors.unexpected_character, pos)
        return tokens.ERROR, end_pos
    end
end

--[[- Lex a single token from an input string.

@param context The current parser context.
@tparam string str The string we're lexing.
@tparam number pos The start position.
@treturn[1] number The id of the parsed token.
@treturn[1] number The start position of this token.
@treturn[1] number The end position of this token.
@treturn[1] string|nil The token's current contents (only given for identifiers)
@treturn[2] nil If there are no more tokens to consume
]]
local function lex_one(context, str, pos)
    while true do
        local start_pos, _, c = find(str, "([%S\r\n])", pos)
        if not start_pos then
            return
        elseif c == "\r" or c == "\n" then
            pos = newline(context, str, start_pos, c)
        else
            local token_id, end_pos, content = lex_token(context, str, start_pos)
            return token_id, start_pos, end_pos, content
        end
    end
end

return {
    lex_one = lex_one,
}
