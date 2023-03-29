-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The bg program", function()
    it("opens a tab in the background", function()
        local openTab = stub(shell, "openTab", function() return 12 end)
        local switchTab = stub(shell, "switchTab")
        capture("bg")
        expect(openTab):called_with("shell")
        expect(switchTab):called(0)
    end)
end)
