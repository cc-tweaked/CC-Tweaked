describe("cc.expect", function()
    local e = require("cc.expect")

    describe("expect", function()
        it("checks a single type", function()
            expect(e.expect(1, "test", "string")):eq("test")
            expect(e.expect(1, 2, "number")):eq(2)

            expect.error(e.expect, 1, nil, "string"):eq("bad argument #1 (expected string, got nil)")
            expect.error(e.expect, 2, 1, "nil"):eq("bad argument #2 (expected nil, got number)")
        end)

        it("checks multiple types", function()
            expect(e.expect(1, "test", "string", "number")):eq("test")
            expect(e.expect(1, 2, "string", "number")):eq(2)

            expect.error(e.expect, 1, nil, "string", "number"):eq("bad argument #1 (expected string or number, got nil)")
            expect.error(e.expect, 2, false, "string", "table", "number", "nil")
                :eq("bad argument #2 (expected string, table or number, got boolean)")
        end)

        it("includes the function name", function()
            local function worker()
                expect(e.expect(1, nil, "string")):eq(true)
            end
            local function trampoline()
                worker()
            end

            expect.error(trampoline):str_match("^[^:]*expect_spec.lua:27: bad argument #1 to 'worker' %(expected string, got nil%)$")
        end)
    end)

    describe("field", function()
        it("checks a single type", function()
            expect(e.field({ k = "test" }, "k", "string")):eq("test")
            expect(e.field({ k = 2 }, "k", "number")):eq(2)

            expect.error(e.field, { k = nil }, "k", "string"):eq("field 'k' missing from table")
            expect.error(e.field, { l = 1 }, "l", "nil"):eq("bad field 'l' (expected nil, got number)")
        end)

        it("checks multiple types", function()
            expect(e.field({ k = "test" }, "k", "string", "number")):eq("test")
            expect(e.field({ k = 2 }, "k", "string", "number")):eq(2)

            expect.error(e.field, { k = nil }, "k", "string", "number")
                :eq("field 'k' missing from table")
            expect.error(e.field, { l = false }, "l", "string", "table", "number", "nil")
                :eq("bad field 'l' (expected string, table or number, got boolean)")
        end)
    end)
end)
