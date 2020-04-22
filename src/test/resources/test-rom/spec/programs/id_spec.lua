local capture = require "test_helpers".capture_program

describe("The id program", function()

    it("displays computer id", function()
        local id = os.getComputerID()

        expect(capture(stub, "id"))
            :matches { ok = true, output = "This is computer #" .. id .. "\n", error = "" }
    end)
end)
