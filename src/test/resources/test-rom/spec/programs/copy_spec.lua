local capture = require "test_helpers".capture_program

describe("The copy program", function()
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("copies a file", function()
        touch("/test-files/copy/a.txt")

        shell.run("copy /test-files/copy/a.txt /test-files/copy/b.txt")

        expect(fs.exists("/test-files/copy/a.txt")):eq(true)
        expect(fs.exists("/test-files/copy/b.txt")):eq(true)
    end)

    it("fails when copying a non-existent file", function()
        expect(capture(stub, "copy nothing destination"))
            :matches { ok = true, output = "", error = "No matching files\n" }
    end)

    it("fails when overwriting an existing file", function()
        touch("/test-files/copy/c.txt")

        expect(capture(stub, "copy /test-files/copy/c.txt /test-files/copy/c.txt"))
            :matches { ok = true, output = "", error = "Destination exists\n" }
    end)

    it("fails when copying into read-only locations", function()
        touch("/test-files/copy/d.txt")

        expect(capture(stub, "copy /test-files/copy/d.txt /rom/test.txt"))
            :matches { ok = true, output = "", error = "Destination is read-only\n" }
    end)

    it("displays the usage when given no arguments", function()
        expect(capture(stub, "copy"))
            :matches { ok = true, output = "Usage: copy <source> <destination>\n", error = "" }
    end)
end)
