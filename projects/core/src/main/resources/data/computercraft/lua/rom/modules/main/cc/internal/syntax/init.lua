-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- The main entrypoint to our Lua parser

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

@local
]]

local expect = require "cc.expect".expect

local lex_one = require "cc.internal.syntax.lexer".lex_one
local parser = require "cc.internal.syntax.parser"
local error_printer = require "cc.internal.error_printer"

local error_sentinel = {}

local function make_context(input)
    expect(1, input, "string")

    local context = {}

    local lines = { 1 }
    function context.line(pos) lines[#lines + 1] = pos end

    function context.get_pos(pos)
        expect(1, pos, "number")
        for i = #lines, 1, -1 do
            local start = lines[i]
            if pos >= start then return i, pos - start + 1 end
        end

        error("Position is <= 0", 2)
    end

    function context.get_line(pos)
        expect(1, pos, "number")
        for i = #lines, 1, -1 do
            local start = lines[i]
            if pos >= start then return input:match("[^\r\n]*", start) end
        end

        error("Position is <= 0", 2)
    end

    return context
end

local function make_lexer(input, context)
    local tokens, last_token = parser.tokens, parser.tokens.COMMENT
    local pos = 1
    return function()
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
    end
end

local function parse(input, start_symbol)
    expect(1, input, "string")
    expect(2, start_symbol, "number")

    local context = make_context(input)
    function context.report(msg, ...)
        expect(1, msg, "table", "function")
        if type(msg) == "function" then msg = msg(...) end
        error_printer(context, msg)
        error(error_sentinel)
    end

    local ok, err = pcall(parser.parse, context, make_lexer(input, context), start_symbol)

    if ok then
        return true
    elseif err == error_sentinel then
        return false
    else
        error(err, 0)
    end
end

--[[- Parse a Lua program, printing syntax errors to the terminal.

@tparam string input The string to parse.
@treturn boolean Whether the string was successfully parsed.
]]
local function parse_program(input) return parse(input, parser.program) end

--[[- Parse a REPL input (either a program or a list of expressions), printing
syntax errors to the terminal.

@tparam string input The string to parse.
@treturn boolean Whether the string was successfully parsed.
]]
local function parse_repl(input)
    expect(1, input, "string")


    local context = make_context(input)

    local last_error = nil
    function context.report(msg, ...)
        expect(1, msg, "table", "function")
        if type(msg) == "function" then msg = msg(...) end
        last_error = msg
        error(error_sentinel)
    end

    local lexer = make_lexer(input, context)

    local parsers = {}
    for i, start_code in ipairs { parser.repl_exprs, parser.program } do
        parsers[i] = coroutine.create(parser.parse)
        assert(coroutine.resume(parsers[i], context, coroutine.yield, start_code))
    end

    -- Run all parsers together in parallel, feeding them one token at a time.
    -- Once all parsers have failed, report the last failure (corresponding to
    -- the longest parse).
    local ok, err = pcall(function()
        local parsers_n = #parsers
        while true do
            local token, start, finish = lexer()

            local all_failed = true
            for i = 1, parsers_n do
                local parser = parsers[i]
                if parser then
                    local ok, err = coroutine.resume(parser, token, start, finish)
                    if ok then
                        -- This parser accepted our input, succeed immediately.
                        if coroutine.status(parser) == "dead" then return end

                        all_failed = false -- Otherwise continue parsing.
                    elseif err ~= error_sentinel then
                        -- An internal error occurred: propagate it.
                        error(err, 0)
                    else
                        -- The parser failed, stub it out so we don't try to continue using it.
                        parsers[i] = false
                    end
                end
            end

            if all_failed then error(error_sentinel) end
        end
    end)

    if ok then
        return true
    elseif err == error_sentinel then
        error_printer(context, last_error)
        return false
    else
        error(err, 0)
    end
end

return {
    parse_program = parse_program,
    parse_repl = parse_repl,
}
