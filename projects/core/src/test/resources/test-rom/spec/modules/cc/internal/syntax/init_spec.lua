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
    local function make_golden(path, print_tokens)
        local file = fs.open("test-rom/spec/modules/cc/internal/syntax/" .. path, "r")
        local contents = file.readAll()
        file.close()

        local i = 1
        for extra, lua, text in helpers.find_golden_tests(contents) do
            it("test #" .. i, function()
                local start = nil
                if #extra > 0 then
                    start = parser[extra:match("^{([a-z_]+)}$")]
                    if not start then
                        fail("Cannot extract start symbol " .. extra)
                    end
                end

                expect(syntax_helpers.capture_parser(lua, print_tokens, start))
                    :describe("For input string <<<\n" .. lua .. "\n>>>")
                    :eq(text)
            end)
            i = i + 1
        end
    end

    describe("the lexer", function() make_golden("lexer_spec.md", true) end)
    describe("the parser", function() make_golden("parser_spec.md", false) end)
    describe("the parser (all states)", function() make_golden("parser_exhaustive_spec.md", false) end)
end)
