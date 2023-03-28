-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The help library", function()
    describe("help.setPath", function()
        it("validates arguments", function()
            help.setPath(help.path())
            expect.error(help.setPath, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("help.lookup", function()
        it("validates arguments", function()
            help.lookup("")
            expect.error(help.lookup, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("help.completeTopic", function()
        it("validates arguments", function()
            help.completeTopic("")
            expect.error(help.completeTopic, nil):eq("bad argument #1 (string expected, got nil)")
        end)

        it("completes topics without extensions", function()
            expect(help.completeTopic("changel")):same { "og" }
            expect(help.completeTopic("turt")):same { "le" }
        end)
    end)
end)
