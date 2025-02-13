-- SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("cc.internal.error_hints", function()
    local error_hints = require "cc.internal.error_hints"

    local function get_tip_for(code)
        local fn = assert(load(code, "=input.lua"))
        local co = coroutine.create(fn)
        local ok, err = coroutine.resume(co)
        expect(ok):eq(false)

        local _, _, err = err:match("^([^:]+):(%d+): (.*)")

        local tip = error_hints.get_tip(err, co, 0)
        return tip and tostring(tip) or nil
    end

    describe("gives hints for 'attempt to OP (a nil value)' errors", function()
        it("suggests alternative globals", function()
            expect(get_tip_for("pront()")):eq("Did you mean: print?")
        end)

        it("suggests alternative locals", function()
            expect(get_tip_for("local foo; fot()")):eq("Did you mean: foo?")
        end)

        it("suggests alternative table keys", function()
            expect(get_tip_for("redstone.getinput()")):eq("Did you mean: getInput?")
        end)

        it("suggests multiple table keys", function()
            expect(get_tip_for("redstone.getAnaloguInput()")):eq("Did you mean: getAnalogInput or getAnalogueInput?")
        end)
    end)
end)
