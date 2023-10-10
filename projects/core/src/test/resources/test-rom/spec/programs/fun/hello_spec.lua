-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The hello program", function()
    it("says hello", function()
        local slowPrint = stub(textutils, "slowPrint", function(...) return print(...) end)
        expect(capture("hello"))
            :matches { ok = true, output = "Hello World!\n", error = "" }
        expect(slowPrint):called(1)
    end)
end)
