local capture = require "test_helpers".capture_program

describe("The hello program", function()

    it("run the program", function()    
        expect(capture(stub, "hello"))
            :matches { ok = true, output = "Hello World!\n", error = "" }
    end)
end)
