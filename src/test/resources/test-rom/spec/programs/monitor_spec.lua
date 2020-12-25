local capture = require "test_helpers".capture_program

describe("The monitor program", function()
    it("displays its usage when given no arguments", function()
        expect(capture(stub, "monitor"))
            :matches { ok = true, output = "Usage: monitor <name> <program> <arguments>\n", error = "" }
    end)
        
    it("changes the text scale with the resolution command", function()
        local r = 1
        stub(peripheral, "call", function(s, f, t) r = t end)
        expect(capture(stub, "monitor", "left", "resolution", "0.5"))
            :matches { ok = true, output = "", error = "" }
        expect(r):equals(0.5)
    end)
end)
