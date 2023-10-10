-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local expect = require "cc.expect".expect
local lex_one = require "cc.internal.syntax.lexer".lex_one
local parser = require "cc.internal.syntax.parser"
local tokens, last_token = parser.tokens, parser.tokens.COMMENT

--- Make a dummy context.
local function make_context(input)
    local lines = { 1 }
    local function line(pos) lines[#lines + 1] = pos end

    local function get_pos(pos)
        for i = #lines, 1, -1 do
            local start = lines[i]
            if pos >= start then return i, pos - start + 1, start end
        end

        error("Position is <= 0", 2)
    end

    return { line = line, get_pos = get_pos, lines = lines }
end

--[[- Run a parser on an input string, capturing its output.

This uses a simplified method of displaying errors (compared with
[`cc.internal.error_printer`]), which is suitable for printing to a file.

@tparam string input The input string to parse.
@tparam[opt=false] boolean print_tokens Whether to print each token as its parsed.
@tparam[opt] number start The start state of the parser.
@treturn string The parser's output
]]
local function capture_parser(input, print_tokens, start)
    expect(1, input, "string")
    expect(2, print_tokens, "boolean", "nil")
    expect(3, start, "number", "nil")

    local error_sentinel = {}
    local out = {}
    local function print(x) out[#out + 1] = tostring(x) end

    local function get_name(token)
        for name, tok in pairs(tokens) do if tok == token then return name end end
        return "?[" .. tostring(token) .. "]"
    end

    local context = make_context(input)
    function context.report(message, ...)
        expect(3, message, "table", "function")
        if type(message) == "function" then message = message(...) end

        for _, msg in ipairs(message) do
            if type(msg) == "table" and msg.tag == "annotate" then
                local line, col = context.get_pos(msg.start_pos)
                local end_line, end_col = context.get_pos(msg.end_pos)

                local contents = input:match("^([^\r\n]*)", context.lines[line])
                print("   |")
                print(("%2d | %s"):format(line, contents))

                local indicator = line == end_line and ("^"):rep(end_col - col + 1) or "^..."
                if #msg.msg > 0 then
                    print(("   | %s%s %s"):format((" "):rep(col - 1), indicator, msg.msg))
                else
                    print(("   | %s%s"):format((" "):rep(col - 1), indicator))
                end
            else
                print(tostring(msg))
            end
        end
    end

    local pos = 1
    local ok, err = xpcall(function()
        return parser.parse(context, function()
            while true do
                local token, start, finish, content = lex_one(context, input, pos)
                if not token then return tokens.EOF, #input + 1, #input + 1 end

                if print_tokens then
                    local start_line, start_col = context.get_pos(start)
                    local end_line, end_col = context.get_pos(finish)
                    local text = input:sub(start, finish)
                    print(("%d:%d-%d:%d %s %s"):format(
                        start_line, start_col, end_line, end_col,
                        get_name(token), content or text:gsub("\n", "<NL>")
                    ))
                end

                pos = finish + 1

                if token < last_token then
                    return token, start, finish
                elseif token == tokens.ERROR then
                    error(error_sentinel)
                end
            end
        end, start)
    end, debug.traceback)

    if not ok and err ~= error_sentinel then
        print(err)
    end

    return table.concat(out, "\n")
end

return { make_context = make_context, capture_parser = capture_parser }
