local capture = require "test_helpers".capture_program

describe("The turtle equip program", function()
    it("errors when not a turtle", function()
        stub(_G, "turtle", nil)

        expect(capture(stub, "/rom/programs/turtle/equip.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)


    it("displays its usage when given no arguments", function()
        stub(_G, "turtle", {})

        expect(capture(stub, "/rom/programs/turtle/equip.lua"))
            :matches { ok = true, output = "Usage: /rom/programs/turtle/equip.lua <slot> <side>\n", error = "" }
    end)

    it("equip nothing", function()
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return 0 end,
        })

        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Nothing to equip\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Nothing to equip\n", error = "" }
    end)

    it("swaps existing upgrades", function()
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return 1 end,
            equipLeft = function() return true end,
            equipRight = function() return true end,
        })

        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Items swapped\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Items swapped\n", error = "" }
    end)

    describe("equips a new upgrade", function()
        local function setup()
            local item_count = 1
            stub(_G, "turtle", {
                select = function() end,
                getItemCount = function() return item_count end,
                equipLeft = function()
                    item_count  = 0
                    return true
                end,
                equipRight = function()
                    item_count = 0
                    return true
                end,
            })
        end

        it("on the left", function()
            setup()
            expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
                :matches { ok = true, output = "Item equipped\n", error = "" }
        end)

        it("on the right", function()
            setup()
            expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
                :matches { ok = true, output = "Item equipped\n", error = "" }
        end)
    end)

    it("handles when an upgrade cannot be equipped", function()
        stub(_G, "turtle", {
            select = function() end,
            getItemCount = function() return 1 end,
            equipLeft = function() return false end,
            equipRight = function() return false end,
        })

        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Item not equippable\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Item not equippable\n", error = "" }
    end)

end)
