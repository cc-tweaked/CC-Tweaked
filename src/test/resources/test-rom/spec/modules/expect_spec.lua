describe("craftos.expect", function()
    local e = require("craftos.expect")

    it("checks a single type", function()
        expect(e.expect(1, "test", "string")):eq(true)
        expect(e.expect(1, 2, "number")):eq(true)

        expect.error(e.expect, 1, nil, "string"):eq("bad argument #1 (expected string, got nil)")
        expect.error(e.expect, 2, 1, "nil"):eq("bad argument #2 (expected nil, got number)")
    end)

    it("checks multiple types", function()
        expect(e.expect(1, "test", "string", "number")):eq(true)
        expect(e.expect(1, 2, "string", "number")):eq(true)

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

        expect.error(trampoline):eq("expect_spec.lua:26: bad argument #1 to 'worker' (expected string, got nil)")
    end)
end)
