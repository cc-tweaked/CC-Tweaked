local capture = require "test_helpers".capture_program

describe("The rename program", function()
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("can rename a file", function()
        touch("/test-files/rename/a.txt")

        shell.run("rename /test-files/rename/a.txt /test-files/rename/b.txt")

        expect(fs.exists("/test-files/rename/a.txt")):eq(false)
        expect(fs.exists("/test-files/rename/b.txt")):eq(true)
    end)

    it("fails when renaming a file which doesn't exist", function()
        expect(capture(stub, "rename nothing destination"))
            :matches { ok = true, output = "", error = "No matching files\n" }
    end)

    it("fails when overwriting an existing file", function()
        touch("/test-files/rename/c.txt")

        expect(capture(stub, "rename /test-files/rename/c.txt /test-files/rename/c.txt"))
            :matches { ok = true, output = "", error = "Destination exists\n" }
    end)

    it("fails when copying to read-only locations", function()
        touch("/test-files/rename/d.txt")

        expect(capture(stub, "rename /test-files/rename/d.txt /rom/test.txt"))
            :matches { ok = true, output = "", error = "Destination is read-only\n" }
    end)

    it("displays the usage when given no arguments", function()
        expect(capture(stub, "rename"))
            :matches { ok = true, output = "Usage: rename <source> <destination>\n", error = "" }
    end)
end)
