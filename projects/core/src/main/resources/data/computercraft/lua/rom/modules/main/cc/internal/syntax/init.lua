--[[- The main entrypoint to our Lua parser

:::warning
This is an internal module and SHOULD NOT be used in your own code. It may
be removed or changed at any time.
:::

@local
]]

local expect = require "cc.expect".expect

local lex_one = require "cc.internal.syntax.lexer".lex_one
local parser = require "cc.internal.syntax.parser"
local report = require "cc.internal.syntax.report"

--[[- Parse an input string, printing syntax errors to the terminal.

@tparam string input The string to parse.
@treturn boolean Whether the string was successfully parsed.
]]
local function parse(input)
    expect(1, input, "string")

    -- Lazy-load the parser.
    local parse, tokens, last_token = parser.parse, parser.tokens, parser.tokens.COMMENT

    local error_sentinel = {}

    local lines = { 1 }
    local function line(pos) lines[#lines + 1] = pos end

    local function get_pos(pos)
        for i = #lines, 1, -1 do
            local start = lines[i]
            if pos >= start then return i, pos - start + 1, start end
        end

        error("Position is <= 0", 2)
    end

    local function report_impl(msg)
        report(input, get_pos, msg)
        error(error_sentinel)
    end

    local context = { line = line, get_pos = get_pos, report = report_impl }

    local pos = 1
    local ok, err = pcall(parse, context, function()
        while true do
            local token, start, finish = lex_one(context, input, pos)
            if not token then return tokens.EOF, #input + 1, #input + 1 end

            pos = finish + 1

            if token < last_token then
                return token, start, finish
            elseif token == tokens.ERROR then
                error(error_sentinel)
            end
        end
    end)

    if ok then
        return true
    elseif err == error_sentinel then
        return false
    else
        error(err, 0)
    end
end

return { parse = parse }
