local capture = require "test_helpers".capture_program

describe("The redstone program", function()

    it("displays its usage when given no argument", function()
        multishell = nil
        
        expect(capture(stub, "redstone"))
            :matches { ok = true, output = "Usages:\nredstone probe\nredstone set <side> <value>\nredstone set <side> <color> <value>\nredstone pulse <side> <count> <period>\n", error = "" }
    end)
end)
