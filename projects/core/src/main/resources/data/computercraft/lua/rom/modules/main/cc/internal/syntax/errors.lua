-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- The error messages reported by our lexer and parser.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

This provides a list of factory methods which take source positions and produce
appropriate error messages targeting that location. These error messages can
then be displayed to the user via [`cc.internal.error_printer`].

@local
]]

local pretty = require "cc.pretty"
local expect = require "cc.expect".expect
local tokens = require "cc.internal.syntax.parser".tokens

local function annotate(start_pos, end_pos, msg)
    if msg == nil and (type(end_pos) == "string" or type(end_pos) == "table" or type(end_pos) == "nil") then
        end_pos, msg = start_pos, end_pos
    end

    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, msg, "string", "table", "nil")

    return { tag = "annotate", start_pos = start_pos, end_pos = end_pos, msg = msg or "" }
end

--- Format a string as a non-highlighted block of code.
--
-- @tparam string msg The code to format.
-- @treturn cc.pretty.Doc The formatted code.
local function code(msg) return pretty.text(msg, colours.lightGrey) end

--- Maps tokens to a more friendly version.
local token_names = setmetatable({
    -- Specific tokens.
    [tokens.IDENT] = "identifier",
    [tokens.NUMBER] = "number",
    [tokens.STRING] = "string",
    [tokens.EOF] = "end of file",
    -- Symbols and keywords
    [tokens.ADD] = code("+"),
    [tokens.AND] = code("and"),
    [tokens.BREAK] = code("break"),
    [tokens.CBRACE] = code("}"),
    [tokens.COLON] = code(":"),
    [tokens.COMMA] = code(","),
    [tokens.CONCAT] = code(".."),
    [tokens.CPAREN] = code(")"),
    [tokens.CSQUARE] = code("]"),
    [tokens.DIV] = code("/"),
    [tokens.DO] = code("do"),
    [tokens.DOT] = code("."),
    [tokens.DOTS] = code("..."),
    [tokens.DOUBLE_COLON] = code("::"),
    [tokens.ELSE] = code("else"),
    [tokens.ELSEIF] = code("elseif"),
    [tokens.END] = code("end"),
    [tokens.EQ] = code("=="),
    [tokens.EQUALS] = code("="),
    [tokens.FALSE] = code("false"),
    [tokens.FOR] = code("for"),
    [tokens.FUNCTION] = code("function"),
    [tokens.GE] = code(">="),
    [tokens.GOTO] = code("goto"),
    [tokens.GT] = code(">"),
    [tokens.IF] = code("if"),
    [tokens.IN] = code("in"),
    [tokens.LE] = code("<="),
    [tokens.LEN] = code("#"),
    [tokens.LOCAL] = code("local"),
    [tokens.LT] = code("<"),
    [tokens.MOD] = code("%"),
    [tokens.MUL] = code("*"),
    [tokens.NE] = code("~="),
    [tokens.NIL] = code("nil"),
    [tokens.NOT] = code("not"),
    [tokens.OBRACE] = code("{"),
    [tokens.OPAREN] = code("("),
    [tokens.OR] = code("or"),
    [tokens.OSQUARE] = code("["),
    [tokens.POW] = code("^"),
    [tokens.REPEAT] = code("repeat"),
    [tokens.RETURN] = code("return"),
    [tokens.SEMICOLON] = code(";"),
    [tokens.SUB] = code("-"),
    [tokens.THEN] = code("then"),
    [tokens.TRUE] = code("true"),
    [tokens.UNTIL] = code("until"),
    [tokens.WHILE] = code("while"),
}, { __index = function(_, name) error("No such token " .. tostring(name), 2) end })

local errors = {}

--------------------------------------------------------------------------------
-- Lexer errors
--------------------------------------------------------------------------------

--[[- A string which ends without a closing quote.

@tparam number start_pos The start position of the string.
@tparam number end_pos The end position of the string.
@tparam string quote The kind of quote (`"` or `'`).
@return The resulting parse error.
]]
function errors.unfinished_string(start_pos, end_pos, quote)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, quote, "string")

    return {
        "This string is not finished. Are you missing a closing quote (" .. code(quote) .. ")?",
        annotate(start_pos, "String started here."),
        annotate(end_pos, "Expected a closing quote here."),
    }
end

--[[- A string which ends with an escape sequence (so a literal `"foo\`). This
is slightly different from [`unfinished_string`], as we don't want to suggest
adding a quote.

@tparam number start_pos The start position of the string.
@tparam number end_pos The end position of the string.
@tparam string quote The kind of quote (`"` or `'`).
@return The resulting parse error.
]]
function errors.unfinished_string_escape(start_pos, end_pos, quote)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, quote, "string")

    return {
        "This string is not finished.",
        annotate(start_pos, "String started here."),
        annotate(end_pos, "An escape sequence was started here, but with nothing following it."),
    }
end

--[[- A long string was never finished.

@tparam number start_pos The start position of the long string delimiter.
@tparam number end_pos The end position of the long string delimiter.
@tparam number ;em The length of the long string delimiter, excluding the first `[`.
@return The resulting parse error.
]]
function errors.unfinished_long_string(start_pos, end_pos, len)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, len, "number")

    return {
        "This string was never finished.",
        annotate(start_pos, end_pos, "String was started here."),
        "We expected a closing delimiter (" .. code("]" .. ("="):rep(len - 1) .. "]") .. ") somewhere after this string was started.",
    }
end

--[[- Malformed opening to a long string (i.e. `[=`).

@tparam number start_pos The start position of the long string delimiter.
@tparam number end_pos The end position of the long string delimiter.
@tparam number len The length of the long string delimiter, excluding the first `[`.
@return The resulting parse error.
]]
function errors.malformed_long_string(start_pos, end_pos, len)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, len, "number")

    return {
        "Incorrect start of a long string.",
        annotate(start_pos, end_pos),
        "Tip: If you wanted to start a long string here, add an extra " .. code("[") .. " here.",
    }
end

--[[- Malformed nesting of a long string.

@tparam number start_pos The start position of the long string delimiter.
@tparam number end_pos The end position of the long string delimiter.
@return The resulting parse error.
]]
function errors.nested_long_str(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        code("[[") .. " cannot be nested inside another " .. code("[[ ... ]]"),
        annotate(start_pos, end_pos),
    }
end

--[[- A malformed numeric literal.

@tparam number start_pos The start position of the number.
@tparam number end_pos The end position of the number.
@return The resulting parse error.
]]
function errors.malformed_number(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "This isn't a valid number.",
        annotate(start_pos, end_pos),
        "Numbers must be in one of the following formats: " .. code("123") .. ", "
        .. code("3.14") .. ", " .. code("23e35") .. ", " .. code("0x01AF") .. ".",
    }
end

--[[- A long comment was never finished.

@tparam number start_pos The start position of the long string delimiter.
@tparam number end_pos The end position of the long string delimiter.
@tparam number len The length of the long string delimiter, excluding the first `[`.
@return The resulting parse error.
]]
function errors.unfinished_long_comment(start_pos, end_pos, len)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")
    expect(3, len, "number")

    return {
        "This comment was never finished.",
        annotate(start_pos, end_pos, "Comment was started here."),
        "We expected a closing delimiter (" .. code("]" .. ("="):rep(len - 1) .. "]") .. ") somewhere after this comment was started.",
    }
end

--[[- `&&` was used instead of `and`.

@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.wrong_and(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected character.",
        annotate(start_pos, end_pos),
        "Tip: Replace this with " .. code("and") .. " to check if both values are true.",
    }
end

--[[- `||` was used instead of `or`.

@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.wrong_or(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected character.",
        annotate(start_pos, end_pos),
        "Tip: Replace this with " .. code("or") .. " to check if either value is true.",
    }
end

--[[- `!=` was used instead of `~=`.

@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.wrong_ne(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected character.",
        annotate(start_pos, end_pos),
        "Tip: Replace this with " .. code("~=") .. " to check if two values are not equal.",
    }
end

--[[- `!` was used instead of `not`.

@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.wrong_not(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected character.",
        annotate(start_pos, end_pos),
        "Tip: Replace this with " .. code("not") .. " to negate a boolean.",
    }
end

--[[- An unexpected character was used.

@tparam number pos The position of this character.
@return The resulting parse error.
]]
function errors.unexpected_character(pos)
    expect(1, pos, "number")
    return {
        "Unexpected character.",
        annotate(pos, "This character isn't usable in Lua code."),
    }
end

--------------------------------------------------------------------------------
-- Expression parsing errors
--------------------------------------------------------------------------------

--[[- A fallback error when we expected an expression but received another token.

@tparam number token The token id.
@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.expected_expression(token, start_pos, end_pos)
    expect(1, token, "number")
    expect(2, start_pos, "number")
    expect(3, end_pos, "number")
    return {
        "Unexpected " .. token_names[token] .. ". Expected an expression.",
        annotate(start_pos, end_pos),
    }
end

--[[- A fallback error when we expected a variable but received another token.

@tparam number token The token id.
@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.expected_var(token, start_pos, end_pos)
    expect(1, token, "number")
    expect(2, start_pos, "number")
    expect(3, end_pos, "number")
    return {
        "Unexpected " .. token_names[token] .. ". Expected a variable name.",
        annotate(start_pos, end_pos),
    }
end

--[[- `=` was used in an expression context.

@tparam number start_pos The start position of the `=` token.
@tparam number end_pos The end position of the `=` token.
@return The resulting parse error.
]]
function errors.use_double_equals(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected " .. code("=") .. " in expression.",
        annotate(start_pos, end_pos),
        "Tip: Replace this with " .. code("==") .. " to check if two values are equal.",
    }
end

--[[- `=` was used after an expression inside a table.

@tparam number start_pos The start position of the `=` token.
@tparam number end_pos The end position of the `=` token.
@return The resulting parse error.
]]
function errors.table_key_equals(start_pos, end_pos)
    expect(1, start_pos, "number")
    expect(2, end_pos, "number")

    return {
        "Unexpected " .. code("=") .. " in expression.",
        annotate(start_pos, end_pos),
        "Tip: Wrap the preceding expression in " .. code("[") .. " and " .. code("]") .. " to use it as a table key.",
    }
end

--[[- There is a trailing comma in this list of function arguments.

@tparam number token The token id.
@tparam number token_start The start position of the token.
@tparam number token_end The end position of the token.
@tparam number prev The start position of the previous entry.
@treturn table The resulting parse error.
]]
function errors.missing_table_comma(token, token_start, token_end, prev)
    expect(1, token, "number")
    expect(2, token_start, "number")
    expect(3, token_end, "number")
    expect(4, prev, "number")

    return {
        "Unexpected " .. token_names[token] .. " in table.",
        annotate(token_start, token_end),
        annotate(prev + 1, prev + 1, "Are you missing a comma here?"),
    }
end

--[[- There is a trailing comma in this list of function arguments.

@tparam number comma_start The start position of the `,` token.
@tparam number comma_end The end position of the `,` token.
@tparam number paren_start The start position of the `)` token.
@tparam number paren_end The end position of the `)` token.
@treturn table The resulting parse error.
]]
function errors.trailing_call_comma(comma_start, comma_end, paren_start, paren_end)
    expect(1, comma_start, "number")
    expect(2, comma_end, "number")
    expect(3, paren_start, "number")
    expect(4, paren_end, "number")

    return {
        "Unexpected " .. code(")") .. " in function call.",
        annotate(paren_start, paren_end),
        annotate(comma_start, comma_end, "Tip: Try removing this " .. code(",") .. "."),
    }
end

--------------------------------------------------------------------------------
-- Statement parsing errors
--------------------------------------------------------------------------------

--[[- A fallback error when we expected a statement but received another token.

@tparam number token The token id.
@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.expected_statement(token, start_pos, end_pos)
    expect(1, token, "number")
    expect(2, start_pos, "number")
    expect(3, end_pos, "number")
    return {
        "Unexpected " .. token_names[token] .. ". Expected a statement.",
        annotate(start_pos, end_pos),
    }
end

--[[- `local function` was used with a table identifier.

@tparam number local_start The start position of the `local` token.
@tparam number local_end The end position of the `local` token.
@tparam number dot_start The start position of the `.` token.
@tparam number dot_end The end position of the `.` token.
@return The resulting parse error.
]]
function errors.local_function_dot(local_start, local_end, dot_start, dot_end)
    expect(1, local_start, "number")
    expect(2, local_end, "number")
    expect(3, dot_start, "number")
    expect(4, dot_end, "number")

    return {
        "Cannot use " .. code("local function") .. " with a table key.",
        annotate(dot_start, dot_end, code(".") .. " appears here."),
        annotate(local_start, local_end, "Tip: " .. "Try removing this " .. code("local") .. " keyword."),
    }
end

--[[- A statement of the form `x.y`

@tparam number token The token id.
@tparam number pos The position right after this name.
@return The resulting parse error.
]]
function errors.standalone_name(token, pos)
    expect(1, token, "number")
    expect(2, pos, "number")

    return {
        "Unexpected " .. token_names[token] .. " after name.",
        annotate(pos),
        "Did you mean to assign this or call it as a function?",
    }
end

--[[- A statement of the form `x.y, z`

@tparam number token The token id.
@tparam number pos The position right after this name.
@return The resulting parse error.
]]
function errors.standalone_names(token, pos)
    expect(1, token, "number")
    expect(2, pos, "number")

    return {
        "Unexpected " .. token_names[token] .. " after name.",
        annotate(pos),
        "Did you mean to assign this?",
    }
end

--[[- A statement of the form `x.y`. This is similar to [`standalone_name`], but
when the next token is on another line.

@tparam number token The token id.
@tparam number pos The position right after this name.
@return The resulting parse error.
]]
function errors.standalone_name_call(token, pos)
    expect(1, token, "number")
    expect(2, pos, "number")

    return {
        "Unexpected " .. token_names[token] .. " after name.",
        annotate(pos + 1, "Expected something before the end of the line."),
        "Tip: Use " .. code("()") .. " to call with no arguments.",
    }
end

--[[- `then` was expected

@tparam number if_start The start position of the `if`/`elseif` keyword.
@tparam number if_end The end position of the `if`/`elseif` keyword.
@tparam number token_pos The current token position.
@return The resulting parse error.
]]
function errors.expected_then(if_start, if_end, token_pos)
    expect(1, if_start, "number")
    expect(2, if_end, "number")
    expect(3, token_pos, "number")

    return {
        "Expected " .. code("then") .. " after if condition.",
        annotate(if_start, if_end, "If statement started here."),
        annotate(token_pos, "Expected " .. code("then") .. " before here."),
    }

end

--[[- `end` was expected

@tparam number block_start The start position of the block.
@tparam number block_end The end position of the block.
@tparam number token The current token position.
@tparam number token_start The current token position.
@tparam number token_end The current token position.
@return The resulting parse error.
]]
function errors.expected_end(block_start, block_end, token, token_start, token_end)
    return {
        "Unexpected " .. token_names[token] .. ". Expected " .. code("end") .. " or another statement.",
        annotate(block_start, block_end, "Block started here."),
        annotate(token_start, token_end, "Expected end of block here."),
    }
end

--[[- An unexpected `end` in a statement.

@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.unexpected_end(start_pos, end_pos)
    return {
        "Unexpected " .. code("end") .. ".",
        annotate(start_pos, end_pos),
        "Your program contains more " .. code("end") .. "s than needed. Check " ..
        "each block (" .. code("if") .. ", " .. code("for") .. ", " ..
        code("function") .. ", ...) only has one " .. code("end") .. ".",
    }
end

--[[- A label statement was opened but not closed.

@tparam number open_start The start position of the opening label.
@tparam number open_end The end position of the opening label.
@tparam number tok_start The start position of the current token.
@return The resulting parse error.
]]
function errors.unclosed_label(open_start, open_end, token, start_pos, end_pos)
    expect(1, open_start, "number")
    expect(2, open_end, "number")
    expect(3, token, "number")
    expect(4, start_pos, "number")
    expect(5, end_pos, "number")

    return {
        "Unexpected " .. token_names[token] .. ".",
        annotate(open_start, open_end, "Label was started here."),
        annotate(start_pos, end_pos, "Tip: Try adding " .. code("::") .. " here."),

    }
end

--------------------------------------------------------------------------------
-- Generic parsing errors
--------------------------------------------------------------------------------

--[[- A fallback error when we can't produce anything more useful.

@tparam number token The token id.
@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.unexpected_token(token, start_pos, end_pos)
    expect(1, token, "number")
    expect(2, start_pos, "number")
    expect(3, end_pos, "number")

    return {
        "Unexpected " .. token_names[token] .. ".",
        annotate(start_pos, end_pos),
    }
end

--[[- A parenthesised expression was started but not closed.

@tparam number open_start The start position of the opening bracket.
@tparam number open_end The end position of the opening bracket.
@tparam number tok_start The start position of the opening bracket.
@return The resulting parse error.
]]
function errors.unclosed_brackets(open_start, open_end, token, start_pos, end_pos)
    expect(1, open_start, "number")
    expect(2, open_end, "number")
    expect(3, token, "number")
    expect(4, start_pos, "number")
    expect(5, end_pos, "number")

    -- TODO: Do we want to be smarter here with where we report the error?
    return {
        "Unexpected " .. token_names[token] .. ". Are you missing a closing bracket?",
        annotate(open_start, open_end, "Brackets were opened here."),
        annotate(start_pos, end_pos, "Unexpected " .. token_names[token] .. " here."),

    }
end

--[[- Expected `(` to open our function arguments.

@tparam number token The token id.
@tparam number start_pos The start position of the token.
@tparam number end_pos The end position of the token.
@return The resulting parse error.
]]
function errors.expected_function_args(token, start_pos, end_pos)
    return {
        "Unexpected " .. token_names[token] .. ". Expected " .. code("(") .. " to start function arguments.",
        annotate(start_pos, end_pos),
    }
end

return errors
