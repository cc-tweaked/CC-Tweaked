-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local capture = require "test_helpers".capture_program

describe("The paint program", function()
    it("displays its usage when given no arguments", function()
        expect(capture("paint"))
            :matches { ok = true, output = "Usage: paint <path>\n", error = "" }
    end)
end)
