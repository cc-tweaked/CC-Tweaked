-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The commands program", function()
    it("displays an error without the commands api", function()
        stub(_G, "commands", nil)
        expect(capture("/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "", error = "Requires a Command Computer.\n" }
    end)

    it("lists commands", function()
        local pagedTabulate = stub(textutils, "pagedTabulate", function(x) print(table.unpack(x)) end)
        stub(_G, "commands", {
            list = function() return { "computercraft" } end,
        })

        expect(capture("/rom/programs/command/commands.lua"))
            :matches { ok = true, output = "Available commands:\ncomputercraft\n", error = "" }
        expect(pagedTabulate):called_with_matching({ "computercraft" })
    end)
end)
