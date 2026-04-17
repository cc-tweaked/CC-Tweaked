// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Colour;
import net.minecraft.nbt.CompoundTag;

public class NetworkedTerminal extends Terminal {
    public NetworkedTerminal(int width, int height, boolean colour) {
        super(width, height, colour);
    }

    public NetworkedTerminal(int width, int height, boolean colour, Runnable changedCallback) {
        super(width, height, colour, changedCallback);
    }

    synchronized TerminalState write() {
        var textContents = new int[width * height];
        var colours = new byte[width * height];
        var paletteBytes = new byte[Palette.PALETTE_SIZE * 3];

        var textIdx = 0;
        var colourIdx = 0;
        var paletteIdx = 0;

        for (var y = 0; y < height; y++) {
            var textLine = this.text[y];
            var textColourLine = this.textColour[y];
            var backColourLine = backgroundColour[y];

            for (var x = 0; x < width; x++) {
                textContents[textIdx++] = textLine.codePointAt(x);
            }

            for (var x = 0; x < width; x++) {
                colours[colourIdx++] = (byte) (
                    getColour(backColourLine.charAt(x), Colour.BLACK) << 4 |
                        getColour(textColourLine.charAt(x), Colour.WHITE)
                );
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            for (var channel : palette.getColour(i)) {
                paletteBytes[paletteIdx++] = (byte) ((int) (channel * 0xFF) & 0xFF);
            }
        }

        assert textIdx == textContents.length;
        assert colourIdx == colours.length;
        assert paletteIdx == paletteBytes.length;

        return new TerminalState(
            colour, width, height, cursorX, cursorY, cursorBlink, cursorColour, cursorBackgroundColour,
            textContents, colours, paletteBytes
        );
    }

    synchronized void read(TerminalState state) {
        resize(state.width, state.height);
        cursorX = state.cursorX;
        cursorY = state.cursorY;
        cursorBlink = state.cursorBlink;

        cursorBackgroundColour = state.cursorBgColour;
        this.cursorColour = state.cursorFgColour;

        var textContents = state.text;
        var colours = state.colours;
        var paletteBytes = state.palette;

        var textIdx = 0;
        var colourIdx = 0;
        var paletteIdx = 0;

        for (var y = 0; y < height; y++) {
            var textLine = this.text[y];
            var textColourLine = this.textColour[y];
            var backColourLine = backgroundColour[y];

            for (var x = 0; x < width; x++) {
                textLine.setCodePoint(x, textContents[textIdx++]);
            }

            for (var x = 0; x < width; x++) {
                var packedColour = colours[colourIdx++];
                backColourLine.setChar(x, BASE_16.charAt((packedColour >> 4) & 0xF));
                textColourLine.setChar(x, BASE_16.charAt(packedColour & 0xF));
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            var r = (paletteBytes[paletteIdx++] & 0xFF) / 255.0;
            var g = (paletteBytes[paletteIdx++] & 0xFF) / 255.0;
            var b = (paletteBytes[paletteIdx++] & 0xFF) / 255.0;
            palette.setColour(i, r, g, b);
        }

        assert textIdx == textContents.length;
        assert colourIdx == colours.length;
        assert paletteIdx == paletteBytes.length;

        setChanged();
    }

    public synchronized CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putInt("term_cursorX", cursorX);
        nbt.putInt("term_cursorY", cursorY);
        nbt.putBoolean("term_cursorBlink", cursorBlink);
        nbt.putInt("term_textColour", cursorColour);
        nbt.putInt("term_bgColour", cursorBackgroundColour);
        for (var n = 0; n < height; n++) {
            nbt.putString("term_text_" + n, text[n].toString());
            nbt.putString("term_textColour_" + n, textColour[n].toString());
            nbt.putString("term_textBgColour_" + n, backgroundColour[n].toString());
        }

        var rgb8 = new int[Palette.PALETTE_SIZE];
        for (var i = 0; i < Palette.PALETTE_SIZE; i++) rgb8[i] = Palette.encodeRGB8(palette.getColour(i));
        nbt.putIntArray("term_palette", rgb8);

        return nbt;
    }

    public synchronized void readFromNBT(CompoundTag nbt) {
        cursorX = nbt.getInt("term_cursorX");
        cursorY = nbt.getInt("term_cursorY");
        cursorBlink = nbt.getBoolean("term_cursorBlink");
        cursorColour = nbt.getInt("term_textColour");
        cursorBackgroundColour = nbt.getInt("term_bgColour");

        for (var n = 0; n < height; n++) {
            text[n].fill(' ');
            if (nbt.contains("term_text_" + n)) {
                text[n].write(nbt.getString("term_text_" + n));
            }
            textColour[n].fill(BASE_16.charAt(cursorColour));
            if (nbt.contains("term_textColour_" + n)) {
                textColour[n].write(nbt.getString("term_textColour_" + n));
            }
            backgroundColour[n].fill(BASE_16.charAt(cursorBackgroundColour));
            if (nbt.contains("term_textBgColour_" + n)) {
                backgroundColour[n].write(nbt.getString("term_textBgColour_" + n));
            }
        }

        if (nbt.contains("term_palette")) {
            var rgb8 = nbt.getIntArray("term_palette");
            if (rgb8.length == Palette.PALETTE_SIZE) {
                for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
                    var colours = Palette.decodeRGB8(rgb8[i]);
                    palette.setColour(i, colours[0], colours[1], colours[2]);
                }
            }
        }

        setChanged();
    }
}
