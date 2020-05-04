describe("The shell", function()
    describe("require", function()
        it("validates arguments", function()
            require("math")
            expect.error(require, nil):eq("bad argument #1 (expected string, got nil)")
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
            expect.error(shell.setDir, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("not existing directory", function()
            expect.error(shell.setDir, "/rom/nothing"):eq("Not a directory")
        end)
    end)

    describe("shell.setPath", function()
        it("validates arguments", function()
            shell.setPath(shell.path())
            expect.error(shell.setPath, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("shell.resolve", function()
        it("validates arguments", function()
            shell.resolve("")
            expect.error(shell.resolve, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("shell.resolveProgram", function()
        it("validates arguments", function()
            shell.resolveProgram("ls")
            expect.error(shell.resolveProgram, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("shell.complete", function()
        it("validates arguments", function()
            shell.complete("ls")
            expect.error(shell.complete, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("shell.setCompletionFunction", function()
        it("validates arguments", function()
            expect.error(shell.setCompletionFunction, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(shell.setCompletionFunction, "", nil):eq("bad argument #2 (expected function, got nil)")
        end)
    end)

    describe("shell.setCompletionFunction", function()
        it("validates arguments", function()
            expect.error(shell.setCompletionFunction, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(shell.setCompletionFunction, "", nil):eq("bad argument #2 (expected function, got nil)")
        end)
    end)

    describe("shell.setAlias", function()
        it("validates arguments", function()
            shell.setAlias("sl", "ls")
            expect.error(shell.setAlias, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(shell.setAlias, "", nil):eq("bad argument #2 (expected string, got nil)")
        end)
    end)

    describe("shell.clearAlias", function()
        it("validates arguments", function()
            shell.clearAlias("sl")
            expect.error(shell.clearAlias, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("shell.switchTab", function()
        it("validates arguments", function()
            expect.error(shell.switchTab, nil):eq("bad argument #1 (expected number, got nil)")
        end)
    end)
end)
