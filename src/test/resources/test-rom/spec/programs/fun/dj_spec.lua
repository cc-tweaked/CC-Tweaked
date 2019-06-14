local capture = require "test_helpers".capture_program

describe("The dj program", function()

    it("displays its usage when given to many arguments", function()      
        expect(capture(stub, "dj a b c"))
            :matches { ok = true, output = "Usages:\ndj play\ndj play <drive>\ndj stop\n", error = "" }
    end)

    it("run the program without arguments", function()      
        expect(capture(stub, "dj"))
            :matches { ok = true, output = "No Music Discs in attached disk drives\n", error = "" }
    end)
end)
