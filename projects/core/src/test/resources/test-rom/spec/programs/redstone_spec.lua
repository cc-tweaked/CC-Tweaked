-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The redstone program", function()
    it("displays its usage when given no arguments", function()
        expect(capture("redstone"))
            :matches { ok = true, output = "Usages:\nredstone probe\nredstone set <side> <value>\nredstone set <side> <color> <value>\nredstone pulse <side> <count> <period>\n", error = "" }
    end)
end)
