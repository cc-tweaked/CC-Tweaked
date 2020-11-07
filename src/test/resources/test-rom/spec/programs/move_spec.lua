local capture = require "test_helpers".capture_program

describe("The move program", function()
    local function cleanup() fs.delete("/test-files/move") end
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("move a file", function()
        cleanup()
        touch("/test-files/move/a.txt")

        shell.run("move /test-files/move/a.txt /test-files/move/b.txt")

        expect(fs.exists("/test-files/move/a.txt")):eq(false)
        expect(fs.exists("/test-files/move/b.txt")):eq(true)
    end)

    it("moves a file to a directory", function()
        cleanup()
        touch("/test-files/move/a.txt")
        fs.makeDir("/test-files/move/a")

        expect(capture(stub, "move /test-files/move/a.txt /test-files/move/a"))
            :matches { ok = true }

        expect(fs.exists("/test-files/move/a.txt")):eq(false)
        expect(fs.exists("/test-files/move/a/a.txt")):eq(true)
    end)

    it("fails when moving a file which doesn't exist", function()
        expect(capture(stub, "move nothing destination"))
            :matches { ok = true, output = "", error = "No matching files\n" }
    end)

    it("fails when overwriting an existing file", function()
        cleanup()
        touch("/test-files/move/a.txt")

        expect(capture(stub, "move /test-files/move/a.txt /test-files/move/a.txt"))
            :matches { ok = true, output = "", error = "Destination exists\n" }
    end)

    it("fails when moving to read-only locations", function()
        cleanup()
        touch("/test-files/move/a.txt")

        expect(capture(stub, "move /test-files/move/a.txt /rom/test.txt"))
            :matches { ok = true, output = "", error = "Destination is read-only\n" }
    end)

    it("fails when moving from read-only locations", function()
        expect(capture(stub, "move /rom/startup.lua /test-files/move/not-exist.txt"))
            :matches { ok = true, output = "", error = "Cannot move read-only file /rom/startup.lua\n" }
    end)

    it("fails when moving mounts", function()
        expect(capture(stub, "move /rom /test-files/move/rom"))
            :matches { ok = true, output = "", error = "Cannot move mount /rom\n" }
    end)

    it("fails when moving a file multiple times", function()
        cleanup()
        touch("/test-files/move/a.txt")
        touch("/test-files/move/b.txt")
        expect(capture(stub, "move /test-files/move/*.txt /test-files/move/c.txt"))
            :matches { ok = true, output = "", error = "Cannot overwrite file multiple times\n" }
    end)

    it("displays the usage with no arguments", function()
        expect(capture(stub, "move"))
            :matches { ok = true, output = "Usage: move <source> <destination>\n", error = "" }
    end)
end)
