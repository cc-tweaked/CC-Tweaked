// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.terminal;

import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.test.core.CallCounter;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.test.core.terminal.TerminalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class TerminalTest {
    @Test
    void testCreation() {
        var terminal = new Terminal(16, 9, true);
        assertEquals(16, terminal.getWidth());
        assertEquals(9, terminal.getHeight());
    }

    @Test
    void testSetAndGetLine() {
        var terminal = new Terminal(16, 9, true);
        terminal.setLine(1, "ABCDEFGHIJKLMNOP", "0123456789abcdef", "fedcba9876543210");
        assertEquals("ABCDEFGHIJKLMNOP", terminal.getLine(1).toString());
        assertEquals("0123456789abcdef", terminal.getTextColourLine(1).toString());
        assertEquals("fedcba9876543210", terminal.getBackgroundColourLine(1).toString());
    }

    @Test
    void testDefaults() {
        var terminal = new Terminal(16, 9, true);
        assertEquals(0, terminal.getCursorX());
        assertEquals(0, terminal.getCursorY());
        assertFalse(terminal.getCursorBlink());
        assertEquals(0, terminal.getTextColour());
        assertEquals(15, terminal.getBackgroundColour());
    }

    @Test
    void testDefaultTextBuffer() {
        assertThat(new Terminal(4, 3, true), textMatches(new String[]{
            "    ",
            "    ",
            "    ",
        }));
    }

    @Test
    void testDefaultTextColourBuffer() {
        assertThat(new Terminal(4, 3, true), textColourMatches(new String[]{
            "0000",
            "0000",
            "0000",
        }));
    }

    @Test
    void testDefaultBackgroundColourBuffer() {
        assertThat(new Terminal(4, 3, true), backgroundColourMatches(new String[]{
            "ffff",
            "ffff",
            "ffff",
        }));
    }

    @Test
    void testZeroSizeBuffers() {
        var x = new String[0];
        assertThat(new Terminal(0, 0, true), allOf(
            textMatches(new String[0]),
            textColourMatches(x),
            backgroundColourMatches(x)
        ));
    }

    @Test
    void testResizeWidthAndHeight() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        terminal.setLine(0, "test", "aaaa", "eeee");
        callCounter.reset();

        terminal.resize(5, 4);

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "test ",
                "     ",
                "     ",
                "     ",
            }),
            textColourMatches(new String[]{
                "aaaa0",
                "00000",
                "00000",
                "00000",
            }), backgroundColourMatches(new String[]{
                "eeeef",
                "fffff",
                "fffff",
                "fffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testResizeSmaller() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        terminal.setLine(0, "test", "aaaa", "eeee");
        terminal.setLine(1, "smol", "aaaa", "eeee");
        terminal.setLine(2, "term", "aaaa", "eeee");
        callCounter.reset();

        terminal.resize(2, 2);

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "te",
                "sm",
            }),
            textColourMatches(new String[]{
                "aa",
                "aa",
            }),
            backgroundColourMatches(new String[]{
                "ee",
                "ee",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testResizeWithSameDimensions() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        var old = new TerminalBufferSnapshot(terminal);
        terminal.resize(4, 3);

        assertThat("Terminal should be unchanged", terminal, old.matches());

        callCounter.assertNotCalled();
    }

    @Test
    void testSetAndGetCursorPos() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorPos(2, 1);

        assertEquals(2, terminal.getCursorX());
        assertEquals(1, terminal.getCursorY());
        callCounter.assertCalledTimes(1);
    }

    @Test
    void testSetCursorPosUnchanged() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorPos(2, 1);
        callCounter.reset();
        terminal.setCursorPos(2, 1);

        assertEquals(2, terminal.getCursorX());
        assertEquals(1, terminal.getCursorY());
        callCounter.assertNotCalled();
    }

    @Test
    void testSetCursorBlink() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorBlink(true);

        assertTrue(terminal.getCursorBlink());
        callCounter.assertCalledTimes(1);
    }

    @Test
    void testSetCursorBlinkUnchanged() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorBlink(true);
        callCounter.reset();
        terminal.setCursorBlink(true);

        assertTrue(terminal.getCursorBlink());
        callCounter.assertNotCalled();
    }

    @Test
    void testSetTextColour() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setTextColour(5);

        assertEquals(terminal.getTextColour(), 5);
        callCounter.assertCalledTimes(1);
    }

    @Test
    void testSetTextColourUnchanged() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setTextColour(5);
        callCounter.reset();
        terminal.setTextColour(5);

        assertEquals(terminal.getTextColour(), 5);
        callCounter.assertNotCalled();
    }

    @Test
    void testSetBackgroundColour() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setBackgroundColour(5);

        assertEquals(terminal.getBackgroundColour(), 5);
        callCounter.assertCalledTimes(1);
    }

    @Test
    void testSetBackgroundColourUnchanged() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setBackgroundColour(5);
        callCounter.reset();
        terminal.setBackgroundColour(5);

        assertEquals(terminal.getBackgroundColour(), 5);
        callCounter.assertNotCalled();
    }

    @Test
    void testBlitFromOrigin() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        blit(terminal, "test", "1234", "abcd");

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "test",
                "    ",
                "    ",
            }), textColourMatches(new String[]{
                "1234",
                "0000",
                "0000",
            }), backgroundColourMatches(new String[]{
                "abcd",
                "ffff",
                "ffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testBlitWithOffset() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorPos(2, 1);
        callCounter.reset();
        blit(terminal, "hi", "11", "ee");

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "    ",
                "  hi",
                "    ",
            }),
            textColourMatches(new String[]{
                "0000",
                "0011",
                "0000",
            }),
            backgroundColourMatches(new String[]{
                "ffff",
                "ffee",
                "ffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testBlitOutOfBounds() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        var old = new TerminalBufferSnapshot(terminal);

        terminal.setCursorPos(2, -5);
        callCounter.reset();
        blit(terminal, "hi", "11", "ee");
        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();

        terminal.setCursorPos(2, 5);
        callCounter.reset();
        blit(terminal, "hi", "11", "ee");
        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();
    }

    @Test
    public void testBlitPartialBuffer() {
        var terminal = new Terminal(4, 3, true);

        var text = LuaValues.encode("123456");
        text.position(1);

        terminal.blit(text, LuaValues.encode("aaaaaa"), LuaValues.encode("aaaaaa"));

        assertThat(terminal.getLine(0).toString(), equalTo("2345"));
    }

    @Test
    void testWriteFromOrigin() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.write("test");

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "test",
                "    ",
                "    ",
            }), textColourMatches(new String[]{
                "0000",
                "0000",
                "0000",
            }), backgroundColourMatches(new String[]{
                "ffff",
                "ffff",
                "ffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testWriteWithOffset() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setCursorPos(2, 1);
        callCounter.reset();
        terminal.write("hi");

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "    ",
                "  hi",
                "    ",
            }),
            textColourMatches(new String[]{
                "0000",
                "0000",
                "0000",
            }),
            backgroundColourMatches(new String[]{
                "ffff",
                "ffff",
                "ffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testWriteOutOfBounds() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        var old = new TerminalBufferSnapshot(terminal);

        terminal.setCursorPos(2, -5);
        callCounter.reset();
        terminal.write("hi");

        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();

        terminal.setCursorPos(2, 5);
        callCounter.reset();
        terminal.write("hi");
        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();
    }

    @Test
    void testScrollUp() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setLine(1, "test", "1111", "eeee");
        callCounter.reset();
        terminal.scroll(1);

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "test",
                "    ",
                "    ",
            }),
            textColourMatches(new String[]{
                "1111",
                "0000",
                "0000",
            }),
            backgroundColourMatches(new String[]{
                "eeee",
                "ffff",
                "ffff",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testScrollDown() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setLine(1, "test", "1111", "eeee");
        callCounter.reset();
        terminal.scroll(-1);

        assertThat(terminal, allOf(
            textMatches(new String[]{
                "    ",
                "    ",
                "test",
            }),
            textColourMatches(new String[]{
                "0000",
                "0000",
                "1111",
            }),
            backgroundColourMatches(new String[]{
                "ffff",
                "ffff",
                "eeee",
            })
        ));

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testScrollZeroLinesUnchanged() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);

        terminal.setLine(1, "test", "1111", "eeee");
        var old = new TerminalBufferSnapshot(terminal);
        callCounter.reset();
        terminal.scroll(0);

        assertThat(terminal, old.matches());

        callCounter.assertNotCalled();
    }

    @Test
    void testClear() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        var old = new TerminalBufferSnapshot(terminal);

        terminal.setLine(1, "test", "1111", "eeee");
        callCounter.reset();
        terminal.clear();

        assertThat(terminal, old.matches());

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testClearLine() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        var old = new TerminalBufferSnapshot(terminal);

        terminal.setLine(1, "test", "1111", "eeee");
        terminal.setCursorPos(0, 1);
        callCounter.reset();
        terminal.clearLine();

        assertThat(terminal, old.matches());

        callCounter.assertCalledTimes(1);
    }

    @Test
    void testClearLineOutOfBounds() {
        var callCounter = new CallCounter();
        var terminal = new Terminal(4, 3, true, callCounter);
        TerminalBufferSnapshot old;

        terminal.setLine(1, "test", "1111", "eeee");
        old = new TerminalBufferSnapshot(terminal);
        terminal.setCursorPos(0, -5);
        callCounter.reset();
        terminal.clearLine();
        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();

        terminal.setLine(1, "test", "1111", "eeee");
        old = new TerminalBufferSnapshot(terminal);
        terminal.setCursorPos(0, 5);
        callCounter.reset();
        terminal.clearLine();
        assertThat(terminal, old.matches());
        callCounter.assertNotCalled();
    }

    @Test
    void testGetColour() {
        // 0 - 9
        assertEquals(0, Terminal.getColour('0', Colour.WHITE));
        assertEquals(1, Terminal.getColour('1', Colour.WHITE));
        assertEquals(8, Terminal.getColour('8', Colour.WHITE));
        assertEquals(9, Terminal.getColour('9', Colour.WHITE));

        // a - f
        assertEquals(10, Terminal.getColour('a', Colour.WHITE));
        assertEquals(11, Terminal.getColour('b', Colour.WHITE));
        assertEquals(14, Terminal.getColour('e', Colour.WHITE));
        assertEquals(15, Terminal.getColour('f', Colour.WHITE));

        // char out of bounds -> use colour enum ordinal
        assertEquals(0, Terminal.getColour('z', Colour.WHITE));
        assertEquals(0, Terminal.getColour('!', Colour.WHITE));
        assertEquals(0, Terminal.getColour('Z', Colour.WHITE));
        assertEquals(5, Terminal.getColour('Z', Colour.LIME));
    }

    private static void blit(Terminal terminal, String text, String fg, String bg) {
        terminal.blit(LuaValues.encode(text), LuaValues.encode(fg), LuaValues.encode(bg));
    }

    private static final class TerminalBufferSnapshot {
        final String[] textLines;
        final String[] textColourLines;
        final String[] backgroundColourLines;

        private TerminalBufferSnapshot(Terminal terminal) {
            textLines = new String[terminal.getHeight()];
            textColourLines = new String[terminal.getHeight()];
            backgroundColourLines = new String[terminal.getHeight()];

            for (var i = 0; i < terminal.getHeight(); i++) {
                textLines[i] = terminal.getLine(i).toString();
                textColourLines[i] = terminal.getTextColourLine(i).toString();
                backgroundColourLines[i] = terminal.getBackgroundColourLine(i).toString();
            }
        }

        public Matcher<Terminal> matches() {
            return allOf(
                textMatches(textLines), textColourMatches(textColourLines), backgroundColourMatches(backgroundColourLines)
            );
        }
    }
}
