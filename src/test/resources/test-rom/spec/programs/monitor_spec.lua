local capture = require "test_helpers".capture_program

describe("The monitor program", function()

    it("displays its usage when given no argument", function()     
        expect(capture(stub, "monitor"))
            :matches { ok = true, output = "Usage: monitor <name> <program> <arguments>\n", error = "" }
    end)
end)
