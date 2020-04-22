local capture = require "test_helpers".capture_program

describe("The time program", function()

    it("displays time", function()
        local time = textutils.formatTime(os.time())
        local day = os.day()

        expect(capture(stub, "time"))
            :matches { ok = true, output = "The time is " .. time .. " on Day " .. day .. "\n", error = "" }
    end)
end)
