-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The shutdown program", function()

    it("run the program", function()
        local sleep = stub(_G, "sleep")
        local shutdown = stub(os, "shutdown")

        expect(capture("shutdown"))
            :matches { ok = true, output = "Goodbye\n", error = "" }

        expect(sleep):called_with(1)
        expect(shutdown):called()
    end)
end)
