local capture = require "test_helpers".capture_program

describe("The turtle equip program", function()

    it("displays the error without the turtle api", function()
        stub(_G,"turtle",nil)
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)


    it("displays its usage when given no argument", function()
        stub(_G,"turtle",{
        })
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua"))
            :matches { ok = true, output = "Usage: equip <slot> <side>\n", error = "" }
    end)

    it("equip nothing", function()
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return 0
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Nothing to equip\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Nothing to equip\n", error = "" }
    end)

    it("swap items", function()
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return 1
            end,
            equipLeft = function()
                return true
            end,
            equipRight = function()
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Items swapped\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Items swapped\n", error = "" }
    end)

    it("equip item", function()
        itemCount = 1
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return itemCount
            end,
            equipLeft = function()
                itemCount  = 0
                return true
            end,
            equipRight = function()
                itemCount = 0
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Item equipped\n", error = "" }
        itemCount = 1
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Item equipped\n", error = "" }
    end)

    it("can't equip item", function()
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return 1
            end,
            equipLeft = function()
                return false
            end,
            equipRight = function()
                return false
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 left"))
            :matches { ok = true, output = "Item not equippable\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/equip.lua 1 right"))
            :matches { ok = true, output = "Item not equippable\n", error = "" }
    end)

end)
