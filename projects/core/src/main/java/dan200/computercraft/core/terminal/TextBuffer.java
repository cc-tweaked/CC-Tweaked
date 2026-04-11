// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.terminal;

import java.nio.ByteBuffer;

public class TextBuffer {
    private final int[] text;

    public TextBuffer(char c, int length) {
        text = new int[length];
        fill(c);
    }

    public TextBuffer(String text) {
        this.text = text.codePoints().toArray();
    }

    public int length() {
        return text.length;
    }

    public void write(String text) {
        write(text, 0);
    }

    public void write(String text, int start) {
        var codepoints = text.codePoints().toArray();

        var pos = start;
        start = Math.max(start, 0);

        var end = Math.min(start + codepoints.length, pos + codepoints.length);
        end = Math.min(end, this.text.length);

        for (var i = start; i < end; i++) {
            this.text[i] = codepoints[i - pos];
        }
    }

    public void write(ByteBuffer text, int start) {
        var pos = start;
        var bufferPos = text.position();

        start = Math.max(start, 0);
        var length = text.remaining();
        var end = Math.min(start + length, pos + length);
        end = Math.min(end, this.text.length);
        for (var i = start; i < end; i++) {
            this.text[i] = text.get(bufferPos + i - pos) & 0xFF;
        }
    }

    public void write(TextBuffer text) {
        var end = Math.min(text.length(), this.text.length);
        for (var i = 0; i < end; i++) {
            this.text[i] = text.codePointAt(i);
        }
    }

    public void fill(char c) {
        fill(c, 0, text.length);
    }

    public void fill(char c, int start, int end) {
        start = Math.max(start, 0);
        end = Math.min(end, text.length);
        for (var i = start; i < end; i++) {
            text[i] = c;
        }
    }

    public char charAt(int i) {
        var codepoint = text[i];
        return codepoint >= 0 && codepoint <= Character.MAX_VALUE ? (char) codepoint : '?';
    }

    public void setChar(int i, char c) {
        if (i >= 0 && i < text.length) {
            text[i] = c;
        }
    }
    public void setCodePoint(int i, int codepoint) {
        if (i >= 0 && i < text.length) {
            text[i] = codepoint;
        }
    }


    public int codePointAt(int i) {
        return text[i];
    }

    @Override
    public String toString() {
        return new String(text, 0, text.length);
    }
}
