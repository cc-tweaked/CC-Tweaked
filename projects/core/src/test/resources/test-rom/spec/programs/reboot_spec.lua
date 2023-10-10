-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The reboot program", function()
    it("sleeps and then reboots", function()
        local sleep = stub(_G, "sleep")
        local reboot = stub(os, "reboot")

        expect(capture("reboot"))
            :matches { ok = true, output = "Goodbye\n", error = "" }

        expect(sleep):called_with(1)
        expect(reboot):called()
    end)
end)
