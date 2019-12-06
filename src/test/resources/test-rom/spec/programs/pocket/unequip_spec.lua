local capture = require "test_helpers".capture_program

describe("The pocket unequip program", function()
    it("errors when not a pocket computer", function()
        stub(_G, "pocket", nil)
        expect(capture(stub, "/rom/programs/pocket/unequip.lua"))
            :matches { ok = true, output = "", error = "Requires a Pocket Computer\n" }
    end)

    it("unequips an upgrade", function()
        stub(_G, "pocket", {
            unequipBack = function() return true end,
        })

        expect(capture(stub, "/rom/programs/pocket/unequip.lua"))
            :matches { ok = true, output = "Item unequipped\n", error = "" }
    end)

    it("handles when an upgrade cannot be equipped", function()
        stub(_G, "pocket", {
            unequipBack = function() return false, "Nothing to remove." end,
        })

        expect(capture(stub, "/rom/programs/pocket/unequip.lua"))
            :matches { ok = true, output = "", error = "Nothing to remove.\n" }
    end)
end)
