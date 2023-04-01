-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The eject program", function()
    it("displays its usage when given no argument", function()
        expect(capture("eject"))
            :matches { ok = true, output = "Usage: eject <drive>\n", error = "" }
    end)

    it("fails when trying to eject a non-drive", function()
        expect(capture("eject /rom"))
            :matches { ok = true, output = "Nothing in /rom drive\n", error = "" }
    end)
end)
