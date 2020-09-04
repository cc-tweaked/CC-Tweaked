/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.terminal;

public class TextBuffer {
    private final char[] m_text;

    public TextBuffer(char c, int length) {
        this.m_text = new char[length];
        for (int i = 0; i < length; i++) {
            this.m_text[i] = c;
        }
    }

    public TextBuffer(String text) {
        this(text, 1);
    }

    public TextBuffer(String text, int repetitions) {
        int textLength = text.length();
        this.m_text = new char[textLength * repetitions];
        for (int i = 0; i < repetitions; i++) {
            for (int j = 0; j < textLength; j++) {
                this.m_text[j + i * textLength] = text.charAt(j);
            }
        }
    }

    public TextBuffer(TextBuffer text) {
        this(text, 1);
    }

    public TextBuffer(TextBuffer text, int repetitions) {
        int textLength = text.length();
        this.m_text = new char[textLength * repetitions];
        for (int i = 0; i < repetitions; i++) {
            for (int j = 0; j < textLength; j++) {
                this.m_text[j + i * textLength] = text.charAt(j);
            }
        }
    }

    public int length() {
        return this.m_text.length;
    }

    public char charAt(int i) {
        return this.m_text[i];
    }

    public String read() {
        return this.read(0, this.m_text.length);
    }

    public String read(int start, int end) {
        start = Math.max(start, 0);
        end = Math.min(end, this.m_text.length);
        int textLength = Math.max(end - start, 0);
        return new String(this.m_text, start, textLength);
    }

    public String read(int start) {
        return this.read(start, this.m_text.length);
    }

    public void write(String text) {
        this.write(text, 0, text.length());
    }

    public void write(String text, int start, int end) {
        int pos = start;
        start = Math.max(start, 0);
        end = Math.min(end, pos + text.length());
        end = Math.min(end, this.m_text.length);
        for (int i = start; i < end; i++) {
            this.m_text[i] = text.charAt(i - pos);
        }
    }

    public void write(String text, int start) {
        this.write(text, start, start + text.length());
    }

    public void write(TextBuffer text) {
        this.write(text, 0, text.length());
    }

    public void write(TextBuffer text, int start, int end) {
        int pos = start;
        start = Math.max(start, 0);
        end = Math.min(end, pos + text.length());
        end = Math.min(end, this.m_text.length);
        for (int i = start; i < end; i++) {
            this.m_text[i] = text.charAt(i - pos);
        }
    }

    public void write(TextBuffer text, int start) {
        this.write(text, start, start + text.length());
    }

    public void fill(char c) {
        this.fill(c, 0, this.m_text.length);
    }

    public void fill(char c, int start, int end) {
        start = Math.max(start, 0);
        end = Math.min(end, this.m_text.length);
        for (int i = start; i < end; i++) {
            this.m_text[i] = c;
        }
    }

    public void fill(char c, int start) {
        this.fill(c, start, this.m_text.length);
    }

    public void fill(String text) {
        this.fill(text, 0, this.m_text.length);
    }

    public void fill(String text, int start, int end) {
        int pos = start;
        start = Math.max(start, 0);
        end = Math.min(end, this.m_text.length);

        int textLength = text.length();
        for (int i = start; i < end; i++) {
            this.m_text[i] = text.charAt((i - pos) % textLength);
        }
    }

    public void fill(String text, int start) {
        this.fill(text, start, this.m_text.length);
    }

    public void fill(TextBuffer text) {
        this.fill(text, 0, this.m_text.length);
    }

    public void fill(TextBuffer text, int start, int end) {
        int pos = start;
        start = Math.max(start, 0);
        end = Math.min(end, this.m_text.length);

        int textLength = text.length();
        for (int i = start; i < end; i++) {
            this.m_text[i] = text.charAt((i - pos) % textLength);
        }
    }

    public void fill(TextBuffer text, int start) {
        this.fill(text, start, this.m_text.length);
    }

    public void setChar(int i, char c) {
        if (i >= 0 && i < this.m_text.length) {
            this.m_text[i] = c;
        }
    }

    @Override
    public String toString() {
        return new String(this.m_text);
    }
}
