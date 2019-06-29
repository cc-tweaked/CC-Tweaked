local capture = require "test_helpers".capture_program

describe("The peripherals program", function()
    it("says when there are no peripherals", function()
        expect(capture(stub, "peripherals" ))
            :matches { ok = true, output = "Attached Peripherals:\nNone\n", error = "" }
    end)
end)
