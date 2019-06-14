local capture = require "test_helpers".capture_program

describe("The craft program", function()

    it("displays the error without the turtle api", function()
        stub(_G,"turtle",nil)
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)

    it("displays the error without the turtle.craft() function", function()
        stub(_G,"turtle",{})
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "Requires a Crafty Turtle\n", error = "" }
    end)

    it("displays its usage when given no argument", function()
        stub(_G,"turtle",{
            craft = function()
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "Usage: craft [number]\n", error = "" }
    end)

    it("displays its usage when given no argument", function()
        stub(_G,"turtle",{
            craft = function()
            end
        })
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua"))
            :matches { ok = true, output = "Usage: craft [number]\n", error = "" }
    end)

    it("craft 2 items", function()
        local itemCount = 3
        stub(_G,"turtle",{
            craft = function()
                itemCount = 1
                return true
            end,
            getItemCount = function()
                return itemCount
            end,
            getSelectedSlot = function()
                return 1
            end,
        })
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua 2"))
            :matches { ok = true, output = "2 items crafted\n", error = "" }
    end)

    it("craft 1 item", function()
        local itemCount = 2
        stub(_G,"turtle",{
            craft = function()
                itemCount = 1
                return true
            end,
            getItemCount = function()
                return itemCount
            end,
            getSelectedSlot = function()
                return 1
            end,
        })
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua 1"))
            :matches { ok = true, output = "1 item crafted\n", error = "" }
    end)

     it("craft no item", function()
        local itemCount = 2
        stub(_G,"turtle",{
            craft = function()
                itemCount = 1
                return false
            end,
            getItemCount = function()
                return itemCount
            end,
            getSelectedSlot = function()
                return 1
            end,
        })
        
        expect(capture(stub, "/rom/programs/turtle/craft.lua 1"))
            :matches { ok = true, output = "No items crafted\n", error = "" }
    end)
end)
