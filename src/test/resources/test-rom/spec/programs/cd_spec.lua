local capture = require "test_helpers".capture_program

describe("The cd program", function()

    it("cd into a directory", function()
        shell.run("cd /rom/programs")
        
        expect(shell.dir()):eq("rom/programs")
    end)

    it("cd into a not existing directory", function()
        expect(capture(stub, "cd /rom/nothing"))
            :matches { ok = true, output = "Not a directory\n", error = "" }
    end)
    
    it("displays the usage with no arguments", function()
        expect(capture(stub, "cd"))
            :matches { ok = true, output = "Usage: cd <path>\n", error = "" }
    end)
end)
