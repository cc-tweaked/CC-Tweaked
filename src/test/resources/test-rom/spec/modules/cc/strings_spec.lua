local with_window = require "test_helpers".with_window

describe("cc.pretty", function()
    local str = require("cc.strings")

    describe("wrap", function()
        it("validates arguments", function()
            str.wrap("test string is long")
            str.wrap("test string is long", 11)
            expect.error(str.wrap, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(str.wrap, "", false):eq("bad argument #2 (expected number, got boolean)")
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

    describe("normalise", function()
        it("validates arguments", function()
            str.wrap("test string is long")
            str.wrap("test string is long", 11)
            expect.error(str.normalise, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(str.normalise, "", false):eq("bad argument #2 (expected number, got boolean)")
        end)

        it("wraps lines", function()
            expect(str.normalise("test string is long", 25)):eq("test string is long      ")

            expect(str.normalise("test string is long", 15)):eq("test string is ")
        end)
    end)
end)
