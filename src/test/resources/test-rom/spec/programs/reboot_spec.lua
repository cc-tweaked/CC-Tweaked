local capture = require "test_helpers".capture_program

describe("The reboot program", function()

    it("run the program", function()
        stub(_G,"sleep",function(time) print(time) end)

        local tOS = {}
        for k,v in pairs(os) do
            tOS[k] = v
        end
        
        function tOS.reboot()
            print("reboot")
        end

        stub(_G,"os",tOS)
        
        expect(capture(stub, "reboot"))
            :matches { ok = true, output = "Goodbye\n1\nreboot\n", error = "" }
    end)
end)
