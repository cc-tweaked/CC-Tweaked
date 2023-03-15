-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local capture = require "test_helpers".capture_program

describe("The edit program", function()

    it("displays its usage when given no argument", function()
        expect(capture("edit"))
            :matches { ok = true, output = "Usage: edit <path>\n", error = "" }
    end)
end)
