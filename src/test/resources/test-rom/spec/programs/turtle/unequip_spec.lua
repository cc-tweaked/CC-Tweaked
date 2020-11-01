local capture = require "test_helpers".capture_program

describe("The turtle unequip program", function()
    it("errors when not a turtle", function()
        stub(_G, "turtle", nil)

        expect(capture(stub, "/rom/programs/turtle/unequip.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)


    it("displays its usage when given no arguments", function()
        stub(_G, "turtle", {})

        expect(capture(stub, "/rom/programs/turtle/unequip.lua"))
            :matches { ok = true, output = "Usage: /rom/programs/turtle/unequip.lua <side>\n", error = "" }
    end)

    it("says when nothing was unequipped", function()
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return 0 end,
            equipRight = function() return true end,
            equipLeft = function() return true end,
        })

        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "Nothing to unequip\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "Nothing to unequip\n", error = "" }
    end)

    it("unequips a upgrade", function()
        local item_count = 0
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return item_count end,
            equipRight = function()
                item_count = 1
                return true
            end,
            equipLeft = function()
                item_count = 1
                return true
            end,
        })

        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "Item unequipped\n", error = "" }
        item_count = 0
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "Item unequipped\n", error = "" }
    end)

    it("fails when the turtle is full", function()
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return 1 end,
            equipRight = function() return true end,
            equipLeft = function() return true end,
        })

        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "No space to unequip item\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "No space to unequip item\n", error = "" }
    end)

end)
