-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The term library", function()
    describe("term.redirect", function()
        it("validates arguments", function()
            expect.error(term.redirect, nil):eq("bad argument #1 (table expected, got nil)")
        end)

        it("prevents redirecting to term", function()
            expect.error(term.redirect, term)
                  :eq("term is not a recommended redirect target, try term.current() instead")
        end)
    end)
end)
