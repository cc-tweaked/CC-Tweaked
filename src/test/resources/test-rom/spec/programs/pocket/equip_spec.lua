local capture = require "test_helpers".capture_program

describe("The pocket equip program", function()

    it("displays the error without the pocket api", function()
        stub(_G,"pocket",nil)
        
        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "", error = "Requires a Pocket Computer\n" }
    end)

    it("run the program", function()
        stub(_G,"pocket",{
            equipBack = function()
                return true
            end
        })
        
        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "Item equipped\n", error = "" }
    end)

    it("failed to equip", function()
        stub(_G,"pocket",{
            equipBack = function()
                return false, "Test123"
            end
        })
        
        expect(capture(stub, "/rom/programs/pocket/equip.lua"))
            :matches { ok = true, output = "", error = "Test123\n" }
    end)
end)
