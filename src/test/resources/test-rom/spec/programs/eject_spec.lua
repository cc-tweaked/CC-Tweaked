local capture = require "test_helpers".capture_program

describe("The eject program", function()
    it("displays its usage when given no argument", function()
        expect(capture(stub, "eject"))
            :matches { ok = true, output = "Usage: eject <drive>\n", error = "" }
    end)

    it("fails when trying to eject a non-drive", function()
        expect(capture(stub, "eject /rom"))
            :matches { ok = true, output = "Nothing in /rom drive\n", error = "" }
    end)
end)
