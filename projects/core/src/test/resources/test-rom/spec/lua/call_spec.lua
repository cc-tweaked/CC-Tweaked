-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("Function calls into \"native\" CC code", function()
    it("supports custom type names", function()
        local value = setmetatable({}, { __name = "some type" })

        expect.error(redstone.setOutput, value):eq("bad argument #1 (string expected, got some type)")
    end)
end)
