local capture = require "test_helpers".capture_program

describe("The refuel program", function()
    local function setup_turtle(fuel_level,fuel_limit,item_count)
        stub(_G, "turtle", {
            getFuelLevel = function()
                return fuel_level
            end,
            getItemCount = function()
                return item_count
            end,
            refuel = function(nLimit)
                item_count = item_count - nLimit
                fuel_level = fuel_level + nLimit
            end,
            select = function()
            end,
            getFuelLimit = function()
                return fuel_limit
            end
        })
    end

    it("displays the error without the turtle api", function()
        stub(_G,"turtle",nil)
        
        expect(capture(stub, "/rom/programs/turtle/refuel.lua"))
            :matches { ok = true, output = "", error = "Requires a Turtle\n" }
    end)


    it("displays its usage when given to many argument", function()
        setup_turtle(0,5,0)
        
        expect(capture(stub, "/rom/programs/turtle/refuel.lua a b"))
            :matches { ok = true, output = "Usage: refuel [number]\n", error = "" }
    end)

    it("not a number as argument", function()
       setup_turtle(0,0,0)
        
       expect(capture(stub, "/rom/programs/turtle/refuel.lua nothing"))
           :matches { ok = true, output = "Invalid limit, expected a number or \"all\"\n", error = "" }
    end)

    it("fuel turtle", function()
       setup_turtle(0,10,5)
        
       expect(capture(stub, "/rom/programs/turtle/refuel.lua 5"))
           :matches { ok = true, output = "Fuel level is 5\n", error = "" }
    end)
    
    it("fuel turtle full", function()
       setup_turtle(0,5,5)
        
       expect(capture(stub, "/rom/programs/turtle/refuel.lua 5"))
           :matches { ok = true, output = "Fuel level is 5\nFuel limit reached\n", error = "" }
    end)
    
    it("infinity fuel", function()
       setup_turtle("unlimited",5,5)
        
       expect(capture(stub, "/rom/programs/turtle/refuel.lua 5"))
           :matches { ok = true, output = "Fuel level is unlimited\n", error = "" }
    end)
end)
