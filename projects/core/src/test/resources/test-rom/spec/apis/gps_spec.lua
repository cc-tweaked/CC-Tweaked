-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The gps library", function()
    describe("gps.locate", function()
        it("validates arguments", function()
            stub(_G, "commands", { getBlockPosition = function()
            end, })

            gps.locate()
            gps.locate(1)
            gps.locate(1, true)

            expect.error(gps.locate, ""):eq("bad argument #1 (number expected, got string)")
            expect.error(gps.locate, 1, ""):eq("bad argument #2 (boolean expected, got string)")
        end)
    end)
end)
