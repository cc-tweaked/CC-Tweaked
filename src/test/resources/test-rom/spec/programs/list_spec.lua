local capture = require "test_helpers".capture_program

describe("The list program", function()

    it("list files", function()
        expect(capture(stub, "list /rom"))
            :matches { ok = true, output = "\n\n", error = "" }
    end)
end)
