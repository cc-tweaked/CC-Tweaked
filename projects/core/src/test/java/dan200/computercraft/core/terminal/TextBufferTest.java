// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextBufferTest {
    @Test
    void testStringConstructor() {
        var textBuffer = new TextBuffer("test");
        assertEquals("test", textBuffer.toString());
    }

    @Test
    void testCharRepetitionConstructor() {
        var textBuffer = new TextBuffer('a', 5);
        assertEquals("aaaaa", textBuffer.toString());
    }

    @Test
    void testLength() {
        var textBuffer = new TextBuffer("test");
        assertEquals(4, textBuffer.length());
    }

    @Test
    void testWrite() {
        var textBuffer = new TextBuffer(' ', 4);
        textBuffer.write("test");
        assertEquals("test", textBuffer.toString());
    }

    @Test
    void testWriteTextBuffer() {
        var source = new TextBuffer("test");
        var target = new TextBuffer("    ");
        target.write(source);
        assertEquals("test", target.toString());
    }

    @Test
    void testWriteFromPos() {
        var textBuffer = new TextBuffer("test");
        textBuffer.write("il", 1);
        assertEquals("tilt", textBuffer.toString());
    }

    @Test
    void testWriteOutOfBounds() {
        var textBuffer = new TextBuffer("test");
        textBuffer.write("abcdefghijklmnop", -5);
        assertEquals("fghi", textBuffer.toString());
    }

    @Test
    void testWriteOutOfBounds2() {
        var textBuffer = new TextBuffer("             ");
        textBuffer.write("Hello, world!", -3);
        assertEquals("lo, world!   ", textBuffer.toString());
    }

    @Test
    void testFill() {
        var textBuffer = new TextBuffer("test");
        textBuffer.fill('c');
        assertEquals("cccc", textBuffer.toString());
    }

    @Test
    void testFillSubstring() {
        var textBuffer = new TextBuffer("test");
        textBuffer.fill('c', 1, 3);
        assertEquals("tcct", textBuffer.toString());
    }

    @Test
    void testFillOutOfBounds() {
        var textBuffer = new TextBuffer("test");
        textBuffer.fill('c', -5, 5);
        assertEquals("cccc", textBuffer.toString());
    }

    @Test
    void testCharAt() {
        var textBuffer = new TextBuffer("test");
        assertEquals('e', textBuffer.charAt(1));
    }

    @Test
    void testSetChar() {
        var textBuffer = new TextBuffer("test");
        textBuffer.setChar(2, 'n');
        assertEquals("tent", textBuffer.toString());
    }

    @Test
    void testSetCharWithNegativeIndex() {
        var textBuffer = new TextBuffer("test");
        textBuffer.setChar(-5, 'n');
        assertEquals("test", textBuffer.toString(), "Buffer should not change after setting char with negative index.");
    }

    @Test
    void testSetCharWithIndexBeyondBufferEnd() {
        var textBuffer = new TextBuffer("test");
        textBuffer.setChar(10, 'n');
        assertEquals("test", textBuffer.toString(), "Buffer should not change after setting char beyond buffer end.");
    }

    @Test
    void testMultipleOperations() {
        var textBuffer = new TextBuffer(' ', 5);
        textBuffer.setChar(0, 'H');
        textBuffer.setChar(1, 'e');
        textBuffer.setChar(2, 'l');
        textBuffer.write("lo", 3);
        assertEquals("Hello", textBuffer.toString(), "TextBuffer failed to persist over multiple operations.");
    }

    @Test
    void testEmptyBuffer() {
        var textBuffer = new TextBuffer("");
        // exception on writing to empty buffer would fail the test
        textBuffer.write("test");
        assertEquals("", textBuffer.toString());
    }
}
