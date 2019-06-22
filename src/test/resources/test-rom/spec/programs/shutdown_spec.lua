local capture = require "test_helpers".capture_program

describe("The shutdown program", function()

    it("run the program", function()
        stub(_G,"sleep",function(time) print(time) end)

        local tOS = {}
        for k,v in pairs(os) do
            tOS[k] = v
        end
        
        function tOS.shutdown()
            print("shutdown")
        end

        stub(_G,"os",tOS)
        
        expect(capture(stub, "shutdown"))
            :matches { ok = true, output = "Goodbye\n1\nshutdown\n", error = "" }
    end)
end)
