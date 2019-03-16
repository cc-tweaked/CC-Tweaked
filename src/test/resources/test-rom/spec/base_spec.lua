describe("The Lua base library", function()
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
