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
end)
