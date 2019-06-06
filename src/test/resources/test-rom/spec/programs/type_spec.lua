local capture = require "test_helpers".capture_program

describe("The type program", function()

    it("displays the usage with no arguments", function()
        expect(capture(stub, "type"))
            :matches { ok = true, output = "Usage: type <paths>\n", error = "" }
    end)

    it("displays the output for a file", function()
        expect(capture(stub, "type /rom/startup.lua"))
            :matches { ok = true, output = "/rom/startup.lua: File\n", error = "" }
    end)

    it("displays the output for a directory", function()
        expect(capture(stub, "type /rom"))
            :matches { ok = true, output = "/rom: Directory\n", error = "" }
    end)

    it("displays the output for a not existing path", function()
        expect(capture(stub, "type /rom/nothing"))
            :matches { ok = true, output = "/rom/nothing: No such path\n", error = "" }
    end)

    it("displays the output for multiple arguments", function()
        expect(capture(stub, "type /rom /rom/startup.lua /rom/nothing"))
            :matches { ok = true, output = "/rom: Directory\n/rom/startup.lua: File\n/rom/nothing: No such path\n", error = "" }
    end)
end)
