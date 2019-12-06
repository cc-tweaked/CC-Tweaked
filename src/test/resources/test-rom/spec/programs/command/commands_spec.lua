local capture = require "test_helpers".capture_program

describe("The commands program", function()
    it("displays an error without the commands api", function()
        stub(_G, "commands", nil)
        expect(capture(stub, "/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "", error = "Requires a Command Computer.\n" }
    end)

    it("lists commands", function()
        local pagedTabulate = stub(textutils, "pagedTabulate", function(x) print(table.unpack(x)) end)
        stub(_G, "commands", {
            list = function() return { "computercraft" } end,
        })

        expect(capture(stub, "/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "Available commands:\ncomputercraft\n", error = "" }
        expect(pagedTabulate):called_with_matching({ "computercraft" })
    end)
end)
