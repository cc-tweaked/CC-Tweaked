/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;

import net.minecraft.nbt.CompoundTag;

public class Terminal {
    private static final String base16 = "0123456789abcdef";
    private final Palette m_palette;
    private final Runnable onChanged;
    private int m_cursorX;
    private int m_cursorY;
    private boolean m_cursorBlink;
    private int m_cursorColour;
    private int m_cursorBackgroundColour;
    private int m_width;
    private int m_height;
    private TextBuffer[] m_text;
    private TextBuffer[] m_textColour;
    private TextBuffer[] m_backgroundColour;
    private boolean m_changed;

    public Terminal(int width, int height) {
        this(width, height, null);
    }

    public Terminal(int width, int height, Runnable changedCallback) {
        this.m_width = width;
        this.m_height = height;
        this.onChanged = changedCallback;

        this.m_cursorColour = 0;
        this.m_cursorBackgroundColour = 15;

        this.m_text = new TextBuffer[this.m_height];
        this.m_textColour = new TextBuffer[this.m_height];
        this.m_backgroundColour = new TextBuffer[this.m_height];
        for (int i = 0; i < this.m_height; i++) {
            this.m_text[i] = new TextBuffer(' ', this.m_width);
            this.m_textColour[i] = new TextBuffer(base16.charAt(this.m_cursorColour), this.m_width);
            this.m_backgroundColour[i] = new TextBuffer(base16.charAt(this.m_cursorBackgroundColour), this.m_width);
        }

        this.m_cursorX = 0;
        this.m_cursorY = 0;
        this.m_cursorBlink = false;

        this.m_changed = false;

        this.m_palette = new Palette();
    }

    public synchronized void reset() {
        this.m_cursorColour = 0;
        this.m_cursorBackgroundColour = 15;
        this.m_cursorX = 0;
        this.m_cursorY = 0;
        this.m_cursorBlink = false;
        this.clear();
        this.setChanged();
        this.m_palette.resetColours();
    }

    public synchronized void clear() {
        for (int y = 0; y < this.m_height; y++) {
            this.m_text[y].fill(' ');
            this.m_textColour[y].fill(base16.charAt(this.m_cursorColour));
            this.m_backgroundColour[y].fill(base16.charAt(this.m_cursorBackgroundColour));
        }
        this.setChanged();
    }

    public final void setChanged() {
        this.m_changed = true;
        if (this.onChanged != null) {
            this.onChanged.run();
        }
    }

    public int getWidth() {
        return this.m_width;
    }

    public int getHeight() {
        return this.m_height;
    }

    public synchronized void resize(int width, int height) {
        if (width == this.m_width && height == this.m_height) {
            return;
        }

        int oldHeight = this.m_height;
        int oldWidth = this.m_width;
        TextBuffer[] oldText = this.m_text;
        TextBuffer[] oldTextColour = this.m_textColour;
        TextBuffer[] oldBackgroundColour = this.m_backgroundColour;

        this.m_width = width;
        this.m_height = height;

        this.m_text = new TextBuffer[this.m_height];
        this.m_textColour = new TextBuffer[this.m_height];
        this.m_backgroundColour = new TextBuffer[this.m_height];
        for (int i = 0; i < this.m_height; i++) {
            if (i >= oldHeight) {
                this.m_text[i] = new TextBuffer(' ', this.m_width);
                this.m_textColour[i] = new TextBuffer(base16.charAt(this.m_cursorColour), this.m_width);
                this.m_backgroundColour[i] = new TextBuffer(base16.charAt(this.m_cursorBackgroundColour), this.m_width);
            } else if (this.m_width == oldWidth) {
                this.m_text[i] = oldText[i];
                this.m_textColour[i] = oldTextColour[i];
                this.m_backgroundColour[i] = oldBackgroundColour[i];
            } else {
                this.m_text[i] = new TextBuffer(' ', this.m_width);
                this.m_textColour[i] = new TextBuffer(base16.charAt(this.m_cursorColour), this.m_width);
                this.m_backgroundColour[i] = new TextBuffer(base16.charAt(this.m_cursorBackgroundColour), this.m_width);
                this.m_text[i].write(oldText[i]);
                this.m_textColour[i].write(oldTextColour[i]);
                this.m_backgroundColour[i].write(oldBackgroundColour[i]);
            }
        }
        this.setChanged();
    }

    public void setCursorPos(int x, int y) {
        if (this.m_cursorX != x || this.m_cursorY != y) {
            this.m_cursorX = x;
            this.m_cursorY = y;
            this.setChanged();
        }
    }

    public int getCursorX() {
        return this.m_cursorX;
    }

    public int getCursorY() {
        return this.m_cursorY;
    }

    public boolean getCursorBlink() {
        return this.m_cursorBlink;
    }

    public void setCursorBlink(boolean blink) {
        if (this.m_cursorBlink != blink) {
            this.m_cursorBlink = blink;
            this.setChanged();
        }
    }

    public int getTextColour() {
        return this.m_cursorColour;
    }

    public void setTextColour(int colour) {
        if (this.m_cursorColour != colour) {
            this.m_cursorColour = colour;
            this.setChanged();
        }
    }

    public int getBackgroundColour() {
        return this.m_cursorBackgroundColour;
    }

    public void setBackgroundColour(int colour) {
        if (this.m_cursorBackgroundColour != colour) {
            this.m_cursorBackgroundColour = colour;
            this.setChanged();
        }
    }

    public Palette getPalette() {
        return this.m_palette;
    }

    public synchronized void blit(String text, String textColour, String backgroundColour) {
        int x = this.m_cursorX;
        int y = this.m_cursorY;
        if (y >= 0 && y < this.m_height) {
            this.m_text[y].write(text, x);
            this.m_textColour[y].write(textColour, x);
            this.m_backgroundColour[y].write(backgroundColour, x);
            this.setChanged();
        }
    }

    public synchronized void write(String text) {
        int x = this.m_cursorX;
        int y = this.m_cursorY;
        if (y >= 0 && y < this.m_height) {
            this.m_text[y].write(text, x);
            this.m_textColour[y].fill(base16.charAt(this.m_cursorColour), x, x + text.length());
            this.m_backgroundColour[y].fill(base16.charAt(this.m_cursorBackgroundColour), x, x + text.length());
            this.setChanged();
        }
    }

    public synchronized void scroll(int yDiff) {
        if (yDiff != 0) {
            TextBuffer[] newText = new TextBuffer[this.m_height];
            TextBuffer[] newTextColour = new TextBuffer[this.m_height];
            TextBuffer[] newBackgroundColour = new TextBuffer[this.m_height];
            for (int y = 0; y < this.m_height; y++) {
                int oldY = y + yDiff;
                if (oldY >= 0 && oldY < this.m_height) {
                    newText[y] = this.m_text[oldY];
                    newTextColour[y] = this.m_textColour[oldY];
                    newBackgroundColour[y] = this.m_backgroundColour[oldY];
                } else {
                    newText[y] = new TextBuffer(' ', this.m_width);
                    newTextColour[y] = new TextBuffer(base16.charAt(this.m_cursorColour), this.m_width);
                    newBackgroundColour[y] = new TextBuffer(base16.charAt(this.m_cursorBackgroundColour), this.m_width);
                }
            }
            this.m_text = newText;
            this.m_textColour = newTextColour;
            this.m_backgroundColour = newBackgroundColour;
            this.setChanged();
        }
    }

    public synchronized void clearLine() {
        int y = this.m_cursorY;
        if (y >= 0 && y < this.m_height) {
            this.m_text[y].fill(' ');
            this.m_textColour[y].fill(base16.charAt(this.m_cursorColour));
            this.m_backgroundColour[y].fill(base16.charAt(this.m_cursorBackgroundColour));
            this.setChanged();
        }
    }

    public synchronized TextBuffer getLine(int y) {
        if (y >= 0 && y < this.m_height) {
            return this.m_text[y];
        }
        return null;
    }

    public synchronized void setLine(int y, String text, String textColour, String backgroundColour) {
        this.m_text[y].write(text);
        this.m_textColour[y].write(textColour);
        this.m_backgroundColour[y].write(backgroundColour);
        this.setChanged();
    }

    public synchronized TextBuffer getTextColourLine(int y) {
        if (y >= 0 && y < this.m_height) {
            return this.m_textColour[y];
        }
        return null;
    }

    public synchronized TextBuffer getBackgroundColourLine(int y) {
        if (y >= 0 && y < this.m_height) {
            return this.m_backgroundColour[y];
        }
        return null;
    }

    /**
     * @deprecated All {@code *Changed()} methods are deprecated: one should pass in a callback instead.
     */
    @Deprecated
    public final boolean getChanged() {
        return this.m_changed;
    }

    public final void clearChanged() {
        this.m_changed = false;
    }

    public synchronized CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putInt("term_cursorX", this.m_cursorX);
        nbt.putInt("term_cursorY", this.m_cursorY);
        nbt.putBoolean("term_cursorBlink", this.m_cursorBlink);
        nbt.putInt("term_textColour", this.m_cursorColour);
        nbt.putInt("term_bgColour", this.m_cursorBackgroundColour);
        for (int n = 0; n < this.m_height; n++) {
            nbt.putString("term_text_" + n, this.m_text[n].toString());
            nbt.putString("term_textColour_" + n, this.m_textColour[n].toString());
            nbt.putString("term_textBgColour_" + n, this.m_backgroundColour[n].toString());
        }
        if (this.m_palette != null) {
            this.m_palette.writeToNBT(nbt);
        }
        return nbt;
    }

    public synchronized void readFromNBT(CompoundTag nbt) {
        this.m_cursorX = nbt.getInt("term_cursorX");
        this.m_cursorY = nbt.getInt("term_cursorY");
        this.m_cursorBlink = nbt.getBoolean("term_cursorBlink");
        this.m_cursorColour = nbt.getInt("term_textColour");
        this.m_cursorBackgroundColour = nbt.getInt("term_bgColour");

        for (int n = 0; n < this.m_height; n++) {
            this.m_text[n].fill(' ');
            if (nbt.contains("term_text_" + n)) {
                this.m_text[n].write(nbt.getString("term_text_" + n));
            }
            this.m_textColour[n].fill(base16.charAt(this.m_cursorColour));
            if (nbt.contains("term_textColour_" + n)) {
                this.m_textColour[n].write(nbt.getString("term_textColour_" + n));
            }
            this.m_backgroundColour[n].fill(base16.charAt(this.m_cursorBackgroundColour));
            if (nbt.contains("term_textBgColour_" + n)) {
                this.m_backgroundColour[n].write(nbt.getString("term_textBgColour_" + n));
            }
        }
        if (this.m_palette != null) {
            this.m_palette.readFromNBT(nbt);
        }
        this.setChanged();
    }

    public static int getColour( char c, Colour def )
    {
        if( c >= '0' && c <= '9' ) return c - '0';
        if( c >= 'a' && c <= 'f' ) return c - 'a' + 10;
        return 15 - def.ordinal();
    }
}
