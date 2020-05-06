local capture = require "test_helpers".capture_program

describe("The move program", function()
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("move a file", function()
        touch("/test-files/move/a.txt")

        shell.run("move /test-files/move/a.txt /test-files/move/b.txt")

        expect(fs.exists("/test-files/move/a.txt")):eq(false)
        expect(fs.exists("/test-files/move/b.txt")):eq(true)
    end)

    it("fails when moving a file which doesn't exist", function()
        expect(capture(stub, "move nothing destination"))
            :matches { ok = true, output = "", error = "No matching files\n" }
    end)

    it("fails when overwriting an existing file", function()
        touch("/test-files/move/c.txt")

        expect(capture(stub, "move /test-files/move/c.txt /test-files/move/c.txt"))
            :matches { ok = true, output = "", error = "Destination exists\n" }
    end)

    it("fails when moving to read-only locations", function()
        touch("/test-files/move/d.txt")

        expect(capture(stub, "move /test-files/move/d.txt /rom/test.txt"))
            :matches { ok = true, output = "", error = "Destination is read-only\n" }
    end)

    it("fails when moving from read-only locations", function()
        expect(capture(stub, "move /rom/startup.lua /test-files/move/d.txt"))
            :matches { ok = true, output = "", error = "Source is read-only\n" }
    end)

    it("fails when moving mounts", function()
        expect(capture(stub, "move /rom /test-files/move/rom"))
            :matches { ok = true, output = "", error = "Can't move mounts\n" }
    end)

    it("displays the usage with no arguments", function()
        expect(capture(stub, "move"))
            :matches { ok = true, output = "Usage: mv <source> <destination>\n", error = "" }
    end)
end)
