local capture = require "test_helpers".capture_program

describe("The commands program", function()

    it("displays the error without the commands api", function()
        stub(_G,"commands",nil)
        
        expect(capture(stub, "/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "", error = "Requires a Command Computer.\n" }
    end)

    it("list commands", function()
        stub(_G,"commands",{
            list = function()
                return {"computercraft"}
            end
        })
        
        expect(capture(stub, "/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "Available commands:\ncomputercraft\n", error = "" }
    end)
end)
