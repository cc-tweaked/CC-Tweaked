-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The cd program", function()
    it("changes into a directory", function()
        local setDir = stub(shell, "setDir")
        capture("cd /rom/programs")
        expect(setDir):called_with("rom/programs")
    end)

    it("does not move into a non-existent directory", function()
        expect(capture("cd /rom/nothing"))
            :matches { ok = true, output = "Not a directory\n", error = "" }
    end)

    it("displays the usage when given no arguments", function()
        expect(capture("cd"))
            :matches { ok = true, output = "Usage: cd <path>\n", error = "" }
    end)
end)
