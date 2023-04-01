-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The multishell program", function()
    describe("multishell.setFocus", function()
        it("validates arguments", function()
            multishell.setFocus(multishell.getFocus())
            expect.error(multishell.setFocus, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("multishell.getTitle", function()
        it("validates arguments", function()
            multishell.getTitle(1)
            expect.error(multishell.getTitle, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("multishell.setTitle", function()
        it("validates arguments", function()
            multishell.setTitle(1, multishell.getTitle(1))
            expect.error(multishell.setTitle, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(multishell.setTitle, 1, nil):eq("bad argument #2 (string expected, got nil)")
        end)
    end)

    describe("multishell.launch", function()
        it("validates arguments", function()
            expect.error(multishell.launch, nil):eq("bad argument #1 (table expected, got nil)")
            expect.error(multishell.launch, _ENV, nil):eq("bad argument #2 (string expected, got nil)")
        end)
    end)
end)
