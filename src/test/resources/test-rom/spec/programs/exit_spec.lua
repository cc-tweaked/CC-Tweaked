local capture = require "test_helpers".capture_program

describe("The exit program", function()

    it("run the program", function()
        expect(capture(stub, "exit"))
            :matches { ok = true, output = "", error = "" }
    end)
end)
