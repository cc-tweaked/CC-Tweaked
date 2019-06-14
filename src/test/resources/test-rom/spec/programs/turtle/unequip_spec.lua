local capture = require "test_helpers".capture_program

describe("The turtle unequip program", function()

    it("displays the error without the turtle api", function()
        stub(_G,"turtle",nil)
        
        expect(capture(stub, "/rom/programs/turtle/unequip.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)


    it("displays its usage when given no argument", function()
        stub(_G,"turtle",{
        })
        
        expect(capture(stub, "/rom/programs/turtle/unequip.lua"))
            :matches { ok = true, output = "Usage: unequip <side>\n", error = "" }
    end)

    it("unequip nothing", function()
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return 0
            end,
            equipRight = function()
                return true
            end,
            equipLeft = function()
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "Nothing to unequip\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "Nothing to unequip\n", error = "" }
    end)

    it("unequip a item", function()
        local itemCount = 0
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return itemCount
            end,
            equipRight = function()
                itemCount = 1
                return true
            end,
            equipLeft = function()
                itemCount = 1
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "Item unequipped\n", error = "" }
        itemCount = 0
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "Item unequipped\n", error = "" }
    end)
    
    it("no space", function()
        stub(_G,"turtle",{
            select = function()
            end,
            getItemCount = function()
                return 1
            end,
            equipRight = function()
                return true
            end,
            equipLeft = function()
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/unequip.lua left"))
            :matches { ok = true, output = "No space to unequip item\n", error = "" }
        expect(capture(stub, "/rom/programs/turtle/unequip.lua right"))
            :matches { ok = true, output = "No space to unequip item\n", error = "" }
    end)

end)
