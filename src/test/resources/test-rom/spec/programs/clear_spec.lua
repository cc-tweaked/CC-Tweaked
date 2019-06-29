local capture = require "test_helpers".capture_program

describe("The clear program", function()
    it("clears the screen", function()
        local clear = stub(term, "clear")
        local setCursorPos = stub(term, "setCursorPos")

        capture(stub, "clear")

        expect(clear):called(1)
        expect(setCursorPos):called_with(1, 1)
    end)
end)
