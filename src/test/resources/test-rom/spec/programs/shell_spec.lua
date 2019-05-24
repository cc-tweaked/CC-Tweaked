describe("The shell", function()
    describe("shell.run", function()
        it("sets the arguments", function()
            local handle = fs.open("test-files/out.txt", "w")
            handle.writeLine("_G.__arg = arg")
            handle.close()

            shell.run("/test-files/out.txt", "arg1", "arg2")
            fs.delete("test-files/out.txt")

            local args = _G.__arg
            _G.__arg = nil

            expect(args):same { [0] = "/test-files/out.txt", "arg1", "arg2" }
        end)
    end)
end)
