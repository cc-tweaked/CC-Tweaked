-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: LicenseRef-CCPL

describe("The window library", function()
    local function mk()
        return window.create(term.current(), 1, 1, 5, 5, false)
    end

    describe("window.create", function()
        it("validates arguments", function()
            local r = mk()
            window.create(r, 1, 1, 5, 5)
            window.create(r, 1, 1, 5, 5, false)

            expect.error(window.create, nil):eq("bad argument #1 (table expected, got nil)")
            expect.error(window.create, r, nil):eq("bad argument #2 (number expected, got nil)")
            expect.error(window.create, r, 1, nil):eq("bad argument #3 (number expected, got nil)")
            expect.error(window.create, r, 1, 1, nil):eq("bad argument #4 (number expected, got nil)")
            expect.error(window.create, r, 1, 1, 1, nil):eq("bad argument #5 (number expected, got nil)")
            expect.error(window.create, r, 1, 1, 1, 1, ""):eq("bad argument #6 (boolean expected, got string)")
        end)
    end)

    describe("Window.blit", function()
        it("validates arguments", function()
            local w = mk()
            w.blit("a", "a", "a")

            expect.error(w.blit, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(w.blit, "", nil):eq("bad argument #2 (string expected, got nil)")
            expect.error(w.blit, "", "", nil):eq("bad argument #3 (string expected, got nil)")
            expect.error(w.blit, "", "", "a"):eq("Arguments must be the same length")
        end)
    end)

    describe("Window.setCursorPos", function()
        it("validates arguments", function()
            local w = mk()
            w.setCursorPos(1, 1)

            expect.error(w.setCursorPos, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.setCursorPos, 1, nil):eq("bad argument #2 (number expected, got nil)")
        end)
    end)

    describe("Window.setCursorBlink", function()
        it("validates arguments", function()
            local w = mk()
            w.setCursorBlink(false)
            expect.error(w.setCursorBlink, nil):eq("bad argument #1 (boolean expected, got nil)")
        end)
    end)

    describe("Window.setTextColour", function()
        it("validates arguments", function()
            local w = mk()
            w.setTextColour(colors.white)

            expect.error(w.setTextColour, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.setTextColour, -5):eq("Invalid color (got -5)")
        end)
    end)

    describe("Window.setPaletteColour", function()
        it("validates arguments", function()
            local w = mk()
            w.setPaletteColour(colors.white, 0, 0, 0)
            w.setPaletteColour(colors.white, 0x000000)

            expect.error(w.setPaletteColour, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.setPaletteColour, -5):eq("Invalid color (got -5)")
            expect.error(w.setPaletteColour, colors.white):eq("bad argument #2 (number expected, got nil)")
            expect.error(w.setPaletteColour, colors.white, 1, false):eq("bad argument #3 (number expected, got boolean)")
            expect.error(w.setPaletteColour, colors.white, 1, nil, 1):eq("bad argument #3 (number expected, got nil)")
            expect.error(w.setPaletteColour, colors.white, 1, 1, nil):eq("bad argument #4 (number expected, got nil)")
        end)
    end)

    describe("Window.getPaletteColour", function()
        it("validates arguments", function()
            local w = mk()
            w.getPaletteColour(colors.white)
            expect.error(w.getPaletteColour, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.getPaletteColour, -5):eq("Invalid color (got -5)")
        end)
    end)

    describe("Window.setBackgroundColour", function()
        it("validates arguments", function()
            local w = mk()
            w.setBackgroundColour(colors.white)

            expect.error(w.setBackgroundColour, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.setBackgroundColour, -5):eq("Invalid color (got -5)")
        end)
    end)

    describe("Window.scroll", function()
        it("validates arguments", function()
            local w = mk()
            w.scroll(0)
            expect.error(w.scroll, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("Window.setVisible", function()
        it("validates arguments", function()
            local w = mk()
            w.setVisible(false)
            expect.error(w.setVisible, nil):eq("bad argument #1 (boolean expected, got nil)")
        end)
    end)

    describe("Window.reposition", function()
        it("validates arguments", function()
            local w = mk()
            w.reposition(1, 1)
            w.reposition(1, 1, 5, 5)
            expect.error(w.reposition, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.reposition, 1, nil):eq("bad argument #2 (number expected, got nil)")
            expect.error(w.reposition, 1, 1, false, 1):eq("bad argument #3 (number expected, got boolean)")
            expect.error(w.reposition, 1, 1, nil, 1):eq("bad argument #3 (number expected, got nil)")
            expect.error(w.reposition, 1, 1, 1, nil):eq("bad argument #4 (number expected, got nil)")
            expect.error(w.reposition, 1, 1, 1, 1, true):eq("bad argument #5 (table expected, got boolean)")
        end)

        it("can change the buffer", function()
            local a, b = mk(), mk()
            local target = window.create(a, 1, 1, a.getSize())

            target.write("Test")
            expect((a.getLine(1))):equal("Test ")
            expect({ a.getCursorPos() }):same { 5, 1 }

            target.reposition(1, 1, nil, nil, b)

            target.redraw()
            expect((a.getLine(1))):equal("Test ")
            expect({ a.getCursorPos() }):same { 5, 1 }

            target.setCursorPos(1, 1) target.write("More")
            expect((a.getLine(1))):equal("Test ")
            expect((b.getLine(1))):equal("More ")
        end)
    end)

    describe("Window.getLine", function()
        it("validates arguments", function()
            local w = mk()
            w.getLine(1)
            local _, y = w.getSize()
            expect.error(w.getLine, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(w.getLine, 0):eq("Line is out of range.")
            expect.error(w.getLine, y + 1):eq("Line is out of range.")
        end)

        it("provides a line's contents", function()
            local w = mk()
            w.blit("test", "aaaa", "4444")
            expect({ w.getLine(1) }):same { "test ", "aaaa0", "4444f" }
        end)
    end)
    describe("Window.setVisible", function()
        it("validates arguments", function()
            local w = mk()
            expect.error(w.setVisible, nil):eq("bad argument #1 (boolean expected, got nil)")
        end)
    end)
    describe("Window.isVisible", function()
         it("gets window visibility", function()
           local w = mk()
           w.setVisible(false)
           expect(w.isVisible()):same(false)
         end)
    end)
end)
