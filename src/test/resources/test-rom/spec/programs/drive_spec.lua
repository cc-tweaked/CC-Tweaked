local capture = require "test_helpers".capture_program

describe("The drive program", function()

    it("run the program", function()
        local nSpace = fs.getFreeSpace("/")
        

        expect(capture(stub, "drive"))
            :matches { ok = true, output = "hdd (" .. (math.floor( nSpace / (100 * 1000) ) / 10) .. "MB remaining)\n", error = "" }
    end)
    
    it("run with not existing path", function()        

        expect(capture(stub, "drive /rom/nothing"))
            :matches { ok = true, output = "No such path\n", error = "" }
    end)
end)
