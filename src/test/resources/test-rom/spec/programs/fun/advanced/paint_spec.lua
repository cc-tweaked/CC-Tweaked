local capture = require "test_helpers".capture_program

describe("The paint program", function()
    it("displays its usage when given no arguments", function()
        expect(capture(stub, "paint"))
            :matches { ok = true, output = "Usage: paint <path>\n", error = "" }
    end)
end)
