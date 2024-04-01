-- SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The vector library", function()
    local vec = vector.new(1, 2, 3)

    describe("vector.add", function()
        it("validates arguments", function()
            expect.error(vec.add, nil, vec):eq("bad argument #1 (vector expected, got nil)")
            expect.error(vec.add, vec, nil):eq("bad argument #2 (vector expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(vector.new(1, 2, 3) + vector.new(6, 4, 2)):eq(vector.new(7, 6, 5))
        end)
    end)

    describe("vector.sub", function()
        it("validates arguments", function()
            expect.error(vec.sub, nil, vec):eq("bad argument #1 (vector expected, got nil)")
            expect.error(vec.sub, vec, nil):eq("bad argument #2 (vector expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(vector.new(6, 4, 2) - vector.new(1, 2, 3)):eq(vector.new(5, 2, -1))
        end)
    end)

    describe("vector.mul", function()
        it("validates arguments", function()
            expect.error(vec.mul, nil, vec):eq("bad argument #1 (vector expected, got nil)")
            expect.error(vec.mul, vec, nil):eq("bad argument #2 (number expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(vector.new(1, 2, 3) * 2):eq(vector.new(2, 4, 6))
        end)
    end)

    describe("vector.div", function()
        it("validates arguments", function()
            expect.error(vec.div, nil, vec):eq("bad argument #1 (vector expected, got nil)")
            expect.error(vec.div, vec, nil):eq("bad argument #2 (number expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(vector.new(1, 2, 3) / 2):eq(vector.new(0.5, 1, 1.5))
        end)
    end)

    describe("vector.unm", function()
        it("validates arguments", function()
            expect.error(vec.unm, nil):eq("bad argument #1 (vector expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(-vector.new(2, 3, 6)):eq(vector.new(-2, -3, -6))
        end)
    end)

    describe("vector.length", function()
        it("validates arguments", function()
            expect.error(vec.length, nil):eq("bad argument #1 (vector expected, got nil)")
        end)

        it("returns the correct value", function()
            expect(vector.new(2, 3, 6):length()):eq(7)
        end)
    end)
end)
