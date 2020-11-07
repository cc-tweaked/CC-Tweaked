local capture = require "test_helpers".capture_program

describe("The exec program", function()
    it("displays an error without the commands api", function()
        stub(_G, "commands", nil)
        expect(capture(stub, "/rom/programs/command/exec.lua"))
            :matches { ok = true, output = "", error = "Requires a Command Computer.\n" }
    end)

    it("displays its usage when given no argument", function()
        stub(_G, "commands", {})
        expect(capture(stub, "/rom/programs/command/exec.lua"))
            :matches { ok = true, output = "", error = "Usage: /rom/programs/command/exec.lua <command>\n" }
    end)

    it("runs a command", function()
        stub(_G, "commands", {
            exec = function() return true, { "Hello World!" } end,
        })

        expect(capture(stub, "/rom/programs/command/exec.lua computercraft"))
            :matches { ok = true, output = "Success\nHello World!\n", error = "" }
    end)

    it("reports command failures", function()
        stub(_G, "commands", {
            exec = function() return false, { "Hello World!" } end,
        })

        expect(capture(stub, "/rom/programs/command/exec.lua computercraft"))
            :matches { ok = true, output = "Hello World!\n", error = "Failed\n" }
    end)
end)
