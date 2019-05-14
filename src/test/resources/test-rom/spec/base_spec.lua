describe("The Lua base library", function()
    describe("expect", function()
        local e = _G.expect

        it("checks a single type", function()
            expect(e(1, "test", "string")):eq(true)
            expect(e(1, 2, "number")):eq(true)

            expect({ pcall(e, 1, nil, "string") })
                :matches { false, "base_spec.lua:9: bad argument #1 (expected string, got nil)"}
            expect({ pcall(e, 2, 1, "nil") })
                :matches { false, "base_spec.lua:11: bad argument #2 (expected nil, got number)"}
        end)

        it("checks multiple types", function()
            expect(e(1, "test", "string", "number")):eq(true)
            expect(e(1, 2, "string", "number")):eq(true)

            expect({ pcall(e, 1, nil, "string", "number") })
                :matches { false, "base_spec.lua:19: bad argument #1 (expected string or number, got nil)"}
            expect({ pcall(e, 2, false, "string", "table", "number", "nil") })
                :matches { false, "base_spec.lua:21: bad argument #2 (expected string, table or number, got boolean)"}
        end)
    end)

    describe("loadfile", function()
        it("prefixes the filename with @", function()
            local info = debug.getinfo(loadfile("/rom/startup.lua"), "S")
            expect(info):matches { short_src = "startup.lua", source = "@startup.lua" }
        end)
    end)

    describe("loadstring", function()
        it("prefixes the chunk name with '='", function()
            local info = debug.getinfo(loadstring("return 1", "name"), "S")
            expect(info):matches { short_src = "name", source = "=name" }
        end)

        it("does not prefix for unnamed chunks", function()
            local info = debug.getinfo(loadstring("return 1"), "S")
            expect(info):matches { short_src = '[string "return 1"]', source = "return 1", }
        end)

        it("does not prefix when already prefixed", function()
            local info = debug.getinfo(loadstring("return 1", "@file.lua"), "S")
            expect(info):matches { short_src = "file.lua", source = "@file.lua" }

            info = debug.getinfo(loadstring("return 1", "=file.lua"), "S")
            expect(info):matches { short_src = "file.lua", source = "=file.lua" }
        end)
    end)

    describe("load", function()
        local function generator(parts)
            return coroutine.wrap(function()
                for i = 1, #parts do coroutine.yield(parts[i]) end
            end)
        end

        it("does not prefix the chunk name with '='", function()
            local info = debug.getinfo(load("return 1", "name"), "S")
            expect(info):matches { short_src = "[string \"name\"]", source = "name" }

            info = debug.getinfo(load(generator { "return 1" }, "name"), "S")
            expect(info):matches { short_src = "[string \"name\"]", source = "name" }
        end)
    end)
end)
