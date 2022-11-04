local with_window = require "test_helpers".with_window

describe("The paintutils library", function()
    -- Verifies that a window's lines are equal to the given table of blit
    -- strings ({{"text", "fg", "bg"}, {"text", "fg", "bg"}...})
    local function window_eq(w, state)
        -- Verification of the size isn't really important in the tests, but
        -- better safe than sorry.
        local _, height = w.getSize()
        expect(#state):eq(height)

        for line = 1, height do
            expect({ w.getLine(line) }):same(state[line])
        end
    end

    describe("paintutils.parseImage", function()
        it("validates arguments", function()
            paintutils.parseImage("")
            expect.error(paintutils.parseImage, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("paintutils.loadImage", function()
        it("validates arguments", function()
            expect.error(paintutils.loadImage, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("paintutils.drawPixel", function()
        it("validates arguments", function()
            expect.error(paintutils.drawPixel, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(paintutils.drawPixel, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(paintutils.drawPixel, 1, 1, false):eq("bad argument #3 (expected number, got boolean)")
        end)
    end)

    describe("paintutils.drawLine", function()
        it("validates arguments", function()
            expect.error(paintutils.drawLine, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(paintutils.drawLine, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(paintutils.drawLine, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
            expect.error(paintutils.drawLine, 1, 1, 1, nil):eq("bad argument #4 (expected number, got nil)")
            expect.error(paintutils.drawLine, 1, 1, 1, 1, false):eq("bad argument #5 (expected number, got boolean)")
        end)

        it("draws a line going across with custom colour", function()
            local w = with_window(3, 2, function()
                paintutils.drawLine(1, 1, 3, 1, colours.red)
            end)

            window_eq(w, {
                { "   ", "000", "eee" },
                { "   ", "000", "fff" },
            })
        end)

        it("draws a line going diagonally with term colour", function()
            local w = with_window(3, 3, function()
                term.setBackgroundColour(colours.red)
                paintutils.drawLine(1, 1, 3, 3)
            end)

            window_eq(w, {
                { "   ", "000", "eff" },
                { "   ", "000", "fef" },
                { "   ", "000", "ffe" },
            })
        end)

        it("draws a line going diagonally from bottom left", function()
            local w = with_window(3, 3, function()
                term.setBackgroundColour(colours.red)
                paintutils.drawLine(1, 3, 3, 1)
            end)

            window_eq(w, {
                { "   ", "000", "ffe" },
                { "   ", "000", "fef" },
                { "   ", "000", "eff" },
            })
        end)
    end)

    describe("paintutils.drawBox", function()
        it("validates arguments", function()
            expect.error(paintutils.drawBox, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(paintutils.drawBox, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(paintutils.drawBox, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
            expect.error(paintutils.drawBox, 1, 1, 1, nil):eq("bad argument #4 (expected number, got nil)")
            expect.error(paintutils.drawBox, 1, 1, 1, 1, false):eq("bad argument #5 (expected number, got boolean)")
        end)

        it("draws a box with term colour", function()
            local w = with_window(3, 3, function()
                term.setBackgroundColour(colours.red)
                paintutils.drawBox(1, 1, 3, 3)
            end)

            window_eq(w, {
                { "   ", "eee", "eee" },
                { "   ", "e0e", "efe" },
                { "   ", "eee", "eee" },
            })
        end)

        it("draws a box with custom colour", function()
            local w = with_window(3, 3, function()
                paintutils.drawBox(1, 1, 3, 3, colours.red)
            end)

            window_eq(w, {
                { "   ", "eee", "eee" },
                { "   ", "e0e", "efe" },
                { "   ", "eee", "eee" },
            })
        end)

        it("draws a box without overwriting existing content", function()
            local w = with_window(3, 3, function()
                term.setCursorPos(2, 2)
                term.write("a")
                paintutils.drawBox(1, 1, 3, 3, colours.red)
            end)

            window_eq(w, {
                { "   ", "eee", "eee" },
                { " a ", "e0e", "efe" },
                { "   ", "eee", "eee" },
            })
        end)
    end)

    describe("paintutils.drawFilledBox", function()
        it("validates arguments", function()
            expect.error(paintutils.drawFilledBox, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(paintutils.drawFilledBox, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(paintutils.drawFilledBox, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
            expect.error(paintutils.drawFilledBox, 1, 1, 1, nil):eq("bad argument #4 (expected number, got nil)")
            expect.error(paintutils.drawFilledBox, 1, 1, 1, 1, false):eq("bad argument #5 (expected number, got boolean)")
        end)

        it("draws a filled box with term colour", function()
            local w = with_window(3, 3, function()
                term.setBackgroundColour(colours.red)
                paintutils.drawFilledBox(1, 1, 3, 3)
            end)

            window_eq(w, {
                { "   ", "eee", "eee" },
                { "   ", "eee", "eee" },
                { "   ", "eee", "eee" },
            })
        end)

        it("draws a filled box with custom colour", function()
            local w = with_window(3, 3, function()
                paintutils.drawFilledBox(1, 1, 3, 3, colours.red)
            end)

            window_eq(w, {
                { "   ", "eee", "eee" },
                { "   ", "eee", "eee" },
                { "   ", "eee", "eee" },
            })
        end)
    end)

    describe("paintutils.drawImage", function()
        it("validates arguments", function()
            expect.error(paintutils.drawImage, nil):eq("bad argument #1 (expected table, got nil)")
            expect.error(paintutils.drawImage, {}, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(paintutils.drawImage, {}, 1, nil):eq("bad argument #3 (expected number, got nil)")
        end)
    end)
end)
