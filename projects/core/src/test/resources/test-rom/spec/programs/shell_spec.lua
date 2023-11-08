-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local with_window = require "test_helpers".with_window

describe("The shell", function()
    describe("require", function()
        it("validates arguments", function()
            require("math")
            expect.error(require, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.execute", function()
        it("parses in arguments verbatim", function()
            shell.execute("/test-rom/data/dump-args", "arg1", "arg 2")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same { [0] = "/test-rom/data/dump-args", "arg1", "arg 2" }
        end)
    end)

    describe("hashbangs", function()
        local function make_hashbang_file(target, filename)
            local tmp = fs.open(filename or "test-files/out.lua", "w")
            tmp.write("#!" .. target)
            tmp.close()
        end

        it("has basic support", function()
            make_hashbang_file("/test-rom/data/dump-args")
            shell.execute("test-files/out.lua", "arg1", "arg2")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same {
                [0] = "/test-rom/data/dump-args",
                "test-files/out.lua",
                "arg1",
                "arg2",
            }
        end)

        it("supports arguments", function()
            make_hashbang_file("/test-rom/data/dump-args \"iArg1 iArg1-2\" iArg2")
            shell.execute("test-files/out.lua", "arg1", "arg2")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same {
                [0] = "/test-rom/data/dump-args",
                "iArg1 iArg1-2",
                "iArg2",
                "test-files/out.lua",
                "arg1",
                "arg2",
            }
        end)

        it("supports recursion", function()
            make_hashbang_file("/test-rom/data/dump-args")
            make_hashbang_file("test-files/out.lua", "test-files/out2.lua")

            shell.execute("test-files/out2.lua", "arg1", "arg2")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same {
                [0] = "/test-rom/data/dump-args",
                "test-files/out.lua",
                "test-files/out2.lua",
                "arg1",
                "arg2",
            }
        end)

        it("returns error for infinite recursion", function()
            make_hashbang_file("test-files/out.lua")
            expect(shell.execute("test-files/out.lua")):eq(false)
        end)

        it("returns error for using the shell", function()
            make_hashbang_file("shell")
            expect(shell.execute("test-files/out.lua")):eq(false)
        end)

        it("allows running a shell with arguments", function()
            make_hashbang_file("shell /test-rom/data/dump-args")
            expect(shell.execute("test-files/out.lua")):eq(true)
        end)
    end)

    describe("shell.run", function()
        it("tokenises the arguments", function()
            shell.run("/test-rom/data/dump-args", "arg1", "arg 2")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same { [0] = "/test-rom/data/dump-args", "arg1", "arg", "2" }
        end)
    end)

    describe("shell.setDir", function()
        it("validates arguments", function()
            shell.setDir(shell.dir())
            expect.error(shell.setDir, nil):eq("bad argument #1 (string expected, got nil)")
        end)

        it("not existing directory", function()
            expect.error(shell.setDir, "/rom/nothing"):eq("Not a directory")
        end)
    end)

    describe("shell.setPath", function()
        it("validates arguments", function()
            shell.setPath(shell.path())
            expect.error(shell.setPath, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.resolve", function()
        it("validates arguments", function()
            shell.resolve("")
            expect.error(shell.resolve, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.resolveProgram", function()
        it("validates arguments", function()
            shell.resolveProgram("ls")
            expect.error(shell.resolveProgram, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.complete", function()
        it("validates arguments", function()
            shell.complete("ls")
            expect.error(shell.complete, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.setCompletionFunction", function()
        it("validates arguments", function()
            expect.error(shell.setCompletionFunction, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(shell.setCompletionFunction, "", nil):eq("bad argument #2 (function expected, got nil)")
        end)
    end)

    describe("shell.setCompletionFunction", function()
        it("validates arguments", function()
            expect.error(shell.setCompletionFunction, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(shell.setCompletionFunction, "", nil):eq("bad argument #2 (function expected, got nil)")
        end)
    end)

    describe("shell.setAlias", function()
        it("validates arguments", function()
            shell.setAlias("sl", "ls")
            expect.error(shell.setAlias, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(shell.setAlias, "", nil):eq("bad argument #2 (string expected, got nil)")
        end)
    end)

    describe("shell.clearAlias", function()
        it("validates arguments", function()
            shell.clearAlias("sl")
            expect.error(shell.clearAlias, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("shell.switchTab", function()
        it("validates arguments", function()
            expect.error(shell.switchTab, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("file uploads", function()
        local function create_file(name, contents)
            local did_read = false
            return {
                getName = function() return name end,
                read = function()
                    if did_read then return end
                    did_read = true
                    return contents
                end,
                close = function() end,
            }
        end
        local function create_files(files) return { getFiles = function() return files end } end

        it("suspends the read prompt", function()
            fs.delete("transfer.txt")

            local win = with_window(32, 5, function()
                local queue = {
                    { "shell" },
                    { "paste", "xyz" },
                    { "file_transfer", create_files { create_file("transfer.txt", "empty file") } },
                }
                local co = coroutine.create(shell.run)
                for _, event in pairs(queue) do assert(coroutine.resume(co, table.unpack(event))) end
            end)

            expect(win.getCursorBlink()):eq(true)

            local lines = {}
            for i = 1, 5 do lines[i] = win.getLine(i):gsub(" +$", "") end
            expect(lines):same {
                "CraftOS 1.9",
                "> xyz",
                "Transferring transfer.txt",
                "> xyz",
                "",
            }

            expect({ win.getCursorPos() }):same { 6, 4 }
        end)
    end)
end)
