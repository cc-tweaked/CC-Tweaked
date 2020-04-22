local capture = require "test_helpers".capture_program

describe("The edit program", function()

    it("displays its usage when given no argument", function()
        expect(capture(stub, "edit"))
            :matches { ok = true, output = "Usage: edit <path>\n", error = "" }
    end)
end)
