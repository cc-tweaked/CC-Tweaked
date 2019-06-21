local capture = require "test_helpers".capture_program

describe("The peripherals program", function()

    it("run the program", function()
        expect(capture(stub, "peripherals" ))
            :matches { ok = true, output = "Attached Peripherals:\nNone\n", error = "" }
    end)
end)
