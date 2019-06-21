local capture = require "test_helpers".capture_program

describe("The fg program", function()

    it("run the program", function()

        expect(capture(stub, "fg" ))
            :matches { ok = true, output = "CraftOS 1.8\n> ", error = "" }
    end)
end)
