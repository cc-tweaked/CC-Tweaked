local capture = require "test_helpers".capture_program

describe("The gps program", function()
    it("displays its usage when given no arguments", function()
        expect(capture(stub, "gps"))
            :matches { ok = true, output = "Usages:\ngps host\ngps host <x> <y> <z>\ngps locate\n", error = "" }
    end)

    it("fails on a pocket computer", function()
        stub(_G, "pocket", {})

        expect(capture(stub, "gps host"))
            :matches { ok = true, output = "GPS Hosts must be stationary\n", error = "" }
    end)

    it("can locate the computer", function()
        local locate = stub(gps, "locate", function() print("Some debugging information.") end)

        expect(capture(stub, "gps locate"))
            :matches { ok = true, output = "Some debugging information.\n", error = "" }
        expect(locate):called_with(2, true)
    end)
end)
