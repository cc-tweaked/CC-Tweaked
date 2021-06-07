local capture = require "test_helpers".capture_program

describe("The monitor program", function()
    it("displays its usage when given no arguments", function()
        expect(capture(stub, "monitor"))
            :matches {
                ok = true,
                output =
                    "Usage:\n" ..
                    "  monitor <name> <program> <arguments>\n" ..
                    "  monitor scale <name> <scale>\n",
                error = "",
            }
    end)

    it("changes the text scale with the scale command", function()
        local r = 1
        stub(peripheral, "call", function(s, f, t) r = t end)
        stub(peripheral, "getType", function() return "monitor" end)
        expect(capture(stub, "monitor", "scale", "left", "0.5"))
            :matches { ok = true, output = "", error = "" }
        expect(r):equals(0.5)
    end)

    it("displays correct error messages", function()
        local r = 1
        stub(peripheral, "call", function(s, f, t) r = t end)
        stub(peripheral, "getType", function(side) return side == "left" and "monitor" or nil end)
        expect(capture(stub, "monitor", "scale", "left"))
            :matches {
                ok = true,
                output =
                    "Usage:\n" ..
                    "  monitor <name> <program> <arguments>\n" ..
                    "  monitor scale <name> <scale>\n",
                error = "",
            }
        expect(capture(stub, "monitor", "scale", "top", "0.5"))
            :matches { ok = true, output = "No monitor named top\n", error = "" }
        expect(capture(stub, "monitor", "scale", "left", "aaa"))
            :matches { ok = true, output = "Invalid scale: aaa\n", error = "" }
        expect(r):equals(1)
    end)
end)
