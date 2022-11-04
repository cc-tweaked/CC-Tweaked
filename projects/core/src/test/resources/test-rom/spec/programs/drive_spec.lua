local capture = require "test_helpers".capture_program

describe("The drive program", function()
    it("run the program", function()
        local getFreeSpace = stub(fs, "getFreeSpace", function() return 1234e4 end)

        expect(capture(stub, "drive"))
            :matches { ok = true, output = "hdd (12.3MB remaining)\n", error = "" }
        expect(getFreeSpace):called(1):called_with("")
    end)

    it("fails on a non-existent path", function()
        expect(capture(stub, "drive /rom/nothing"))
            :matches { ok = true, output = "No such path\n", error = "" }
    end)
end)
