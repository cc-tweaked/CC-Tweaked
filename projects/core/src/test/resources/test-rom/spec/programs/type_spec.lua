local capture = require "test_helpers".capture_program

describe("The type program", function()

    it("displays the usage with no arguments", function()
        expect(capture(stub, "type"))
            :matches { ok = true, output = "Usage: type <path>\n", error = "" }
    end)

    it("displays the output for a file", function()
        expect(capture(stub, "type /rom/startup.lua"))
            :matches { ok = true, output = "file\n", error = "" }
    end)

    it("displays the output for a directory", function()
        expect(capture(stub, "type /rom"))
            :matches { ok = true, output = "directory\n", error = "" }
    end)

    it("displays the output for a not existing path", function()
        expect(capture(stub, "type /rom/nothing"))
            :matches { ok = true, output = "No such path\n", error = "" }
    end)

end)
