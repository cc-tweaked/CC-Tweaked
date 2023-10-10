-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("cc.expect", function()
    local e = require("cc.expect")

    describe("expect", function()
        it("checks a single type", function()
            expect(e.expect(1, "test", "string")):eq("test")
            expect(e.expect(1, 2, "number")):eq(2)

            expect.error(e.expect, 1, nil, "string"):eq("bad argument #1 (string expected, got nil)")
            expect.error(e.expect, 2, 1, "nil"):eq("bad argument #2 (nil expected, got number)")
        end)

        it("checks multiple types", function()
            expect(e.expect(1, "test", "string", "number")):eq("test")
            expect(e.expect(1, 2, "string", "number")):eq(2)

            expect.error(e.expect, 1, nil, "string", "number"):eq("bad argument #1 (string or number expected, got nil)")
            expect.error(e.expect, 2, false, "string", "table", "number", "nil")
                :eq("bad argument #2 (string, table or number expected, got boolean)")
        end)

        it("includes the function name", function()
            local function worker()
                expect(e.expect(1, nil, "string")):eq(true)
            end
            local function trampoline()
                worker()
            end

            expect.error(trampoline):str_match("^[^:]*expect_spec.lua:31: bad argument #1 to 'worker' %(string expected, got nil%)$")
        end)

        it("supports custom type names", function()
            local value = setmetatable({}, { __name = "some type" })

            expect.error(e.expect, 1, value, "string"):eq("bad argument #1 (string expected, got some type)")
        end)
    end)

    describe("field", function()
        it("checks a single type", function()
            expect(e.field({ k = "test" }, "k", "string")):eq("test")
            expect(e.field({ k = 2 }, "k", "number")):eq(2)

            expect.error(e.field, { k = nil }, "k", "string"):eq("field 'k' missing from table")
            expect.error(e.field, { l = 1 }, "l", "nil"):eq("bad field 'l' (nil expected, got number)")
        end)

        it("checks multiple types", function()
            expect(e.field({ k = "test" }, "k", "string", "number")):eq("test")
            expect(e.field({ k = 2 }, "k", "string", "number")):eq(2)

            expect.error(e.field, { k = nil }, "k", "string", "number")
                :eq("field 'k' missing from table")
            expect.error(e.field, { l = false }, "l", "string", "table", "number", "nil")
                :eq("bad field 'l' (string, table or number expected, got boolean)")
        end)
    end)

    describe("range", function()
        it("works fith full args", function()
            expect(e.range(1, 1, 1)):eq(1)
            expect(e.range(2, 1, 3)):eq(2)

            expect.error(e.range, 2, 0, 1):eq("number outside of range (expected 2 to be within 0 and 1)")
            expect.error(e.range, 0, 1, 2):eq("number outside of range (expected 0 to be within 1 and 2)")
            local NaN = 0 / 0
            expect.error(e.range, NaN, 1, 2):eq(("number outside of range (expected %s to be within 1 and 2)"):format(tostring(NaN)))
        end)

        it("fills in min and max if they are nil", function()
            expect(e.range(1, 1)):eq(1)
            expect(e.range(2, nil, 3)):eq(2)
            expect(e.range(2)):eq(2)

            expect.error(e.range, 2, nil, 1):eq("number outside of range (expected 2 to be within -inf and 1)")
            expect.error(e.range, 0, 1):eq("number outside of range (expected 0 to be within 1 and inf)")
        end)
    end)
end)
