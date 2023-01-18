local helpers = require "test_helpers"

describe("cc.internal.syntax.parser", function()
    local syntax = require "cc.internal.syntax"

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

                    expect(syntax.parse(contents)):describe(file):eq(true)
                end)
            end)
        end
    end)
end)
