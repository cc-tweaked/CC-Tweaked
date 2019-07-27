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

    it("try to move a not existing file", function()
        expect(capture(stub, "move nothing destination"))
            :matches { ok = true, output = "", error = "No matching files\n" }
    end)

    it("displays the usage with no arguments", function()
        expect(capture(stub, "move"))
            :matches { ok = true, output = "Usage: mv <source> <destination>\n", error = "" }
    end)
end)
