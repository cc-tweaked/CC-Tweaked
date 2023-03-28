-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The keys library", function()
    describe("keys.getName", function()
        it("validates arguments", function()
            keys.getName(1)
            expect.error(keys.getName, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)
end)
