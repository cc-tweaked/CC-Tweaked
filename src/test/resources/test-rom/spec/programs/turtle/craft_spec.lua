local capture = require "test_helpers".capture_program

describe("The craft program", function()
    it("errors when not a turtle", function()
        stub(_G, "turtle", nil)

        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)

    it("fails when turtle.craft() is unavailable", function()
        stub(_G, "turtle", {})

        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "Requires a Crafty Turtle\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        stub(_G, "turtle", { craft = function() end })

        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "Usage: /rom/programs/turtle/craft.lua all|<number>\n", error = "" }
    end)

    it("displays its usage when given incorrect arguments", function()
        stub(_G, "turtle", { craft = function() end })

        expect(capture(stub, "/rom/programs/turtle/craft.lua a"))
            :matches { ok = true, output = "Usage: /rom/programs/turtle/craft.lua all|<number>\n", error = "" }
    end)

    it("crafts multiple items", function()
        local item_count = 3
        stub(_G, "turtle", {
            craft = function()
                item_count = 1
                return true
            end,
            getItemCount = function() return item_count end,
            getSelectedSlot = function() return 1 end,
        })

        expect(capture(stub, "/rom/programs/turtle/craft.lua 2"))
            :matches { ok = true, output = "2 items crafted\n", error = "" }
    end)

    it("craft a single item", function()
        local item_count = 2
        stub(_G, "turtle", {
            craft = function()
                item_count = 1
                return true
            end,
            getItemCount = function() return item_count end,
            getSelectedSlot = function() return 1 end,
        })

        expect(capture(stub, "/rom/programs/turtle/craft.lua 1"))
            :matches { ok = true, output = "1 item crafted\n", error = "" }
    end)

    it("crafts no items", function()
        local item_count = 2
        stub(_G, "turtle", {
            craft = function()
                item_count = 1
                return false
            end,
            getItemCount = function() return item_count end,
            getSelectedSlot = function() return 1 end,
        })

        expect(capture(stub, "/rom/programs/turtle/craft.lua 1"))
            :matches { ok = true, output = "No items crafted\n", error = "" }
    end)

    it("crafts all items", function()
        stub(_G, "turtle", {
            craft = function()
                return true
            end,
            getItemCount = function() return 17 end,
            getSelectedSlot = function() return 1 end,
        })

        expect(capture(stub, "/rom/programs/turtle/craft.lua all"))
            :matches { ok = true, output = "17 items crafted\n", error = "" }
    end)
end)
