local capture = require "test_helpers".capture_program

describe("The help program", function()

    it("displays its usage when given a wrong argument", function()      
        expect(capture(stub, "help nothing"))
            :matches { ok = true, output = "No help available\n", error = "" }
    end)
end)
