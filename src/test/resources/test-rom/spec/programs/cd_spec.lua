local capture = require "test_helpers".capture_program

describe("The cd program", function()
    it("changes into a directory", function()
        local setDir = stub(shell, "setDir")
        capture(stub, "cd /rom/programs")
        expect(setDir):called_with("rom/programs")
    end)

    it("does not move into a non-existent directory", function()
        expect(capture(stub, "cd /rom/nothing"))
            :matches { ok = true, output = "Not a directory\n", error = "" }
    end)

    it("displays the usage when given no arguments", function()
        expect(capture(stub, "cd"))
            :matches { ok = true, output = "Usage: cd <path>\n", error = "" }
    end)
end)
