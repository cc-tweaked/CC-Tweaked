local with_window = require "test_helpers".with_window

describe("The Lua base library", function()
    describe("sleep", function()
        it("validates arguments", function()
            sleep(0)
            sleep(nil)

            expect.error(sleep, false):eq("bad argument #1 (expected number, got boolean)")
        end)
    end)

    describe("write", function()
        it("validates arguments", function()
            write("")
            expect.error(write, nil):eq("bad argument #1 (expected string or number, got nil)")
        end)

        it("writes numbers", function()
            local w = with_window(5, 5, function() write(123) end)
            expect(w.getLine(1)):eq("123  ")
        end)

        it("writes strings", function()
            local w = with_window(5, 5, function() write("abc") end)
            expect(w.getLine(1)):eq("abc  ")
        end)
    end)

    describe("loadfile", function()
        local loadfile = _G.native_loadfile or loadfile

        local function make_file()
            local tmp = fs.open("test-files/out.lua", "w")
            tmp.write("return _ENV")
            tmp.close()
        end

        it("validates arguments", function()
            loadfile("")
            loadfile("", "")
            loadfile("", "", {})

            expect.error(loadfile, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(loadfile, "", false):eq("bad argument #2 (expected string, got boolean)")
            expect.error(loadfile, "", "", false):eq("bad argument #3 (expected table, got boolean)")
        end)

        it("prefixes the filename with @", function()
            local info = debug.getinfo(loadfile("/rom/startup.lua"), "S")
            expect(info):matches { short_src = "startup.lua", source = "@startup.lua" }
        end)

        it("loads a file with the global environment", function()
            make_file()
            expect(loadfile("test-files/out.lua")()):eq(_G)
        end)

        it("loads a file with a specific environment", function()
            make_file()
            local env = {}
            expect(loadfile("test-files/out.lua", nil, env)()):eq(env)
        end)

        it("supports the old-style argument form", function()
            make_file()
            local env = {}
            expect(loadfile("test-files/out.lua", env)()):eq(env)
        end)
    end)

    describe("dofile", function()
        it("validates arguments", function()
            expect.error(dofile, ""):eq("File not found")
            expect.error(dofile, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("load", function()
        it("validates arguments", function()
            load("")
            load(function()
            end)
            load("", "")
            load("", "", "")
            load("", "", "", _ENV)

            expect.error(load, nil):eq("bad argument #1 (expected function or string, got nil)")
            expect.error(load, "", false):eq("bad argument #2 (expected string, got boolean)")
            expect.error(load, "", "", false):eq("bad argument #3 (expected string, got boolean)")
            expect.error(load, "", "", "", false):eq("bad argument #4 (expected table, got boolean)")
        end)

        local function generator(parts)
            return coroutine.wrap(function()
                for i = 1, #parts do
                    coroutine.yield(parts[i])
                end
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
