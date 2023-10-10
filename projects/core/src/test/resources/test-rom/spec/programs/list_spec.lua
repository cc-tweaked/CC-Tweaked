-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The list program", function()
    it("lists files", function()
        local pagedTabulate = stub(textutils, "pagedTabulate")
        capture("list /rom")
        expect(pagedTabulate):called_with_matching(
            colors.green, { "apis", "autorun", "help", "modules", "programs" },
            colors.white, { "motd.txt", "startup.lua" }
        )
    end)

    it("fails on a non-existent directory", function()
        expect(capture("list /rom/nothing"))
            :matches { ok = true, output = "", error = "Not a directory\n" }
    end)

    it("fails on a file", function()
        expect(capture("list /rom/startup.lua"))
            :matches { ok = true, output = "", error = "Not a directory\n" }
    end)
end)
