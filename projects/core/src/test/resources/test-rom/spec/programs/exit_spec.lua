-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The exit program", function()
    it("exits the shell", function()
        local exit = stub(shell, "exit")
        expect(capture("exit")):matches { ok = true, combined = "" }
        expect(exit):called(1)
    end)
end)
