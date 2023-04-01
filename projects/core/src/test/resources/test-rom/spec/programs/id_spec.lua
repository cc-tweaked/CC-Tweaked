-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The id program", function()

    it("displays computer id", function()
        local id = os.getComputerID()

        expect(capture("id"))
            :matches { ok = true, output = "This is computer #" .. id .. "\n", error = "" }
    end)
end)
