local capture = require "test_helpers".capture_program

describe("The gps program", function()

    it("displays its usage when given no argument", function()
        
        expect(capture(stub, "gps"))
            :matches { ok = true, output = "Usages:\ngps host\ngps host <x> <y> <z>\ngps locate\n", error = "" }
    end)

    it("try to run with pocket computer", function()
        stub(_G,"pocket",{})

        expect(capture(stub, "gps host"))
            :matches { ok = true, output = "GPS Hosts must be stationary\n", error = "" }
    end)
    
    it("locate", function()
        stub(_G,"gps",{
            locate = function()
                print("locate")
            end
        })

        expect(capture(stub, "gps locate"))
            :matches { ok = true, output = "locate\n", error = "" }
    end)
end)
