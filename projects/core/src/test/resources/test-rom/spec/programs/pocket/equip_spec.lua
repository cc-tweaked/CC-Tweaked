local capture = require "test_helpers".capture_program

describe("The pocket equip program", function()
    it("errors when not a pocket computer", function()
        stub(_G, "pocket", nil)
        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "", error = "Requires a Pocket Computer\n" }
    end)

    it("can equip an upgrade", function()
        stub(_G, "pocket", {
            equipBack = function() return true end,
        })

        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "Item equipped\n", error = "" }
    end)

    it("handles when an upgrade cannot be equipped", function()
        stub(_G, "pocket", {
            equipBack = function() return false, "Cannot equip this item." end,
        })

        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "", error = "Cannot equip this item.\n" }
    end)
end)
