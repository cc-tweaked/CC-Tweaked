-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The peripherals program", function()
    it("says when there are no peripherals", function()
        stub(peripheral, 'getNames', function() return {} end)
        expect(capture("peripherals"))
            :matches { ok = true, output = "Attached Peripherals:\nNone\n", error = "" }
    end)
end)
