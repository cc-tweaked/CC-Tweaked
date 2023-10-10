-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local helpers = require "test_helpers"

describe("cc.internal.syntax", function()
    local syntax = require "cc.internal.syntax"
    local parser = require "cc.internal.syntax.parser"
    local syntax_helpers = require "modules.cc.internal.syntax.syntax_helpers"

    describe("can parse all of CC's Lua files", function()
        local function list_dir(path)
            if not path then path = "/" end
            for _, child in pairs(fs.list(path)) do
                child = fs.combine(path, child)

                if fs.isDir(child) then list_dir(child)
                elseif child:sub(-4) == ".lua" then coroutine.yield(child)
                end
            end
        end

        for file in coroutine.wrap(list_dir) do
            it(file, function()
                helpers.with_window(50, 10, function()
                    local h = fs.open(file, "r")
                    local contents = h.readAll()
                    h.close()

                    expect(syntax.parse_program(contents)):describe(file):eq(true)
                end)
            end)
        end
    end)

    -- We specify most of the parser's behaviour as golden tests. A little nasty
    -- (it's more of an end-to-end test), but much easier to write!
    local function describe_golden(name, path, print_tokens)
        helpers.describe_golden(name, "test-rom/spec/modules/cc/internal/syntax/" .. path, function(lua, extra)
            local start = nil
            if #extra > 0 then
                start = parser[extra:match("^{([a-z_]+)}$")]
                if not start then
                    fail("Cannot extract start symbol " .. extra)
                end
            end

            return syntax_helpers.capture_parser(lua, print_tokens, start)
        end)
    end

    describe_golden("the lexer", "lexer_spec.md", true)
    describe_golden("the parser", "parser_spec.md", false)
    describe_golden("the parser (all states)", "parser_exhaustive_spec.md", false)

    describe("the REPL input parser", function()
        it("returns true when accepted by both parsers", function()
            helpers.with_window(50, 10, function()
                expect(syntax.parse_repl("print(x)")):eq(true)
            end)
        end)

        it("returns true when accepted by one parser", function()
            helpers.with_window(50, 10, function()
                expect(syntax.parse_repl("x")):eq(true)
            end)
        end)
    end)
end)
