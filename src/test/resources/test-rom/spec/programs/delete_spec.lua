local capture = require "test_helpers".capture_program

describe("The rm program", function()
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("deletes one file", function()
        touch("/test-files/a.txt")

        shell.run("rm /test-files/a.txt")

        expect(fs.exists("/test-files/a.txt")):eq(false)
    end)

    it("deletes many files", function()
        touch("/test-files/a.txt")
        touch("/test-files/b.txt")
        touch("/test-files/c.txt")

        shell.run("rm /test-files/a.txt /test-files/b.txt")

        expect(fs.exists("/test-files/a.txt")):eq(false)
        expect(fs.exists("/test-files/b.txt")):eq(false)
        expect(fs.exists("/test-files/c.txt")):eq(true)
    end)

    it("deletes a glob", function()
        touch("/test-files/a.txt")
        touch("/test-files/b.txt")

        shell.run("rm /test-files/*.txt")

        expect(fs.exists("/test-files/a.txt")):eq(false)
        expect(fs.exists("/test-files/b.txt")):eq(false)
    end)

    it("displays the usage with no arguments", function()
        expect(capture(stub, "rm"))
            :matches { ok = true, output = "Usage: rm <paths>\n", error = "" }
    end)

    it("errors when trying to delete a read-only file", function()
        expect(capture(stub, "rm /rom/startup.lua"))
            :matches { ok = true, output = "", error = "Cannot delete read-only file /rom/startup.lua\n" }
    end)

    it("errors when trying to delete the root mount", function()
        expect(capture(stub, "rm /")):matches {
            ok = true,
            output = "To delete its contents run rm /*\n",
            error = "Cannot delete mount /\n",
        }
    end)

    it("errors when a glob fails to match", function()
        expect(capture(stub, "rm", "never-existed"))
            :matches { ok = true, output = "", error = "never-existed: No matching files\n" }
    end)
end)
