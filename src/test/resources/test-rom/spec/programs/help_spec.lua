local capture = require "test_helpers".capture_program

describe("The help program", function()
    it("errors when there is no such help file", function()
        expect(capture(stub, "help nothing"))
            :matches { ok = true, error = "No help available\n", output = "" }
    end)
end)
