describe("The mkdir program", function()
    it("creates a directory", function()
        fs.delete("/test-files")

        shell.run("mkdir /test-files/a")

        expect(fs.isDir("/test-files/a")):eq(true)
    end)

    it("creates many directories", function()
        fs.delete("/test-files")

        shell.run("mkdir /test-files/a /test-files/b")

        expect(fs.isDir("/test-files/a")):eq(true)
        expect(fs.isDir("/test-files/b")):eq(true)
    end)

    it("can be completed", function()
        fs.delete("/test-files")
        fs.makeDir("/test-files/a")
        fs.makeDir("/test-files/b")
        io.open("/test-files.a.txt", "w"):close()

        local complete = shell.getCompletionInfo()["rom/programs/mkdir.lua"].fnComplete
        expect(complete(shell, 1, "/test-files/", {})):same { "a/", "a", "b/", "b" }
        expect(complete(shell, 2, "/test-files/", { "/" })):same { "a/", "a", "b/", "b" }
    end)
end)
