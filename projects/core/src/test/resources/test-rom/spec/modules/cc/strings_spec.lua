-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("cc.strings", function()
    local str = require("cc.strings")

    describe("wrap", function()
        it("validates arguments", function()
            str.wrap("test string is long")
            str.wrap("test string is long", 11)
            expect.error(str.wrap, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(str.wrap, "", false):eq("bad argument #2 (number expected, got boolean)")

            expect.error(str.wrap, "", 0):eq("number outside of range (expected 0 to be within 1 and inf)")
        end)

        it("wraps lines", function()
            expect(str.wrap("test string is long")[1]):eq("test string is long")

            expect(str.wrap("test string is long", 15)[1]):eq("test string is ")
            expect(str.wrap("test string is long", 15)[2]):eq("long")

            expect(str.wrap("test string is long", 12)[1]):eq("test string ")
            expect(str.wrap("test string is long", 12)[2]):eq("is long")

            expect(str.wrap("test string is long", 11)[1]):eq("test string")
            expect(str.wrap("test string is long", 11)[2]):eq("is long")
        end)
    end)

    describe("ensure_width", function()
        it("validates arguments", function()
            str.wrap("test string is long")
            str.wrap("test string is long", 11)
            expect.error(str.ensure_width, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(str.ensure_width, "", false):eq("bad argument #2 (number expected, got boolean)")
        end)

        it("pads lines", function()
            expect(str.ensure_width("test string is long", 25)):eq("test string is long      ")
        end)
        it("truncates lines", function()
            expect(str.ensure_width("test string is long", 15)):eq("test string is ")
        end)
    end)

    describe("split", function()
        it("splits with empty segments", function()
            expect(str.split("", "%-")):same { "" }
            expect(str.split("-", "%-")):same { "", "" }
            expect(str.split("---", "%-")):same { "", "", "", "" }
            expect(str.split("-a", "%-")):same { "", "a" }
            expect(str.split("a-", "%-")):same { "a", "" }
        end)

        it("cannot split with an empty separator", function()
            expect.error(str.split, "abc", ""):eq("separator is empty")
        end)

        it("splits on patterns", function()
            expect(str.split("a.bcd      ef", "%W+")):same { "a", "bcd", "ef" }
        end)

        it("splits on literal strings", function()
            expect(str.split("a-bcd-ef", "-", true)):same { "a", "bcd", "ef" }
        end)

        it("accepts a limit", function()
            expect(str.split("foo-bar-baz-qux-quyux", "-", true, 3)):same { "foo", "bar", "baz-qux-quyux" }
            expect(str.split("foo-bar-baz", "-", true, 5)):same { "foo", "bar", "baz" }
            expect(str.split("foo-bar-baz", "-", true, 1)):same { "foo-bar-baz" }
            expect(str.split("foo-bar-baz", "-", true, 1)):same { "foo-bar-baz" }
        end)
    end)
end)
