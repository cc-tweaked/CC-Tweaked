local capture = require "test_helpers".capture_program

describe("The exit program", function()
    it("exits the shell", function()
        local exit = stub(shell, "exit")
        expect(capture(stub, "exit")):matches { ok = true, combined = "" }
        expect(exit):called(1)
    end)
end)
