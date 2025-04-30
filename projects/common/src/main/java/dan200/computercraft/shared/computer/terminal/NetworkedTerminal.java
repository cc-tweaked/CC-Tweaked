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
        var contents = new byte[width * height * 2 + Palette.PALETTE_SIZE * 3];
        var idx = 0;

        for (var y = 0; y < height; y++) {
            var text = this.text[y];
            var textColour = this.textColour[y];
            var backColour = backgroundColour[y];

            for (var x = 0; x < width; x++) contents[idx++] = (byte) (text.charAt(x) & 0xFF);
            for (var x = 0; x < width; x++) {
                contents[idx++] = (byte) (getColour(backColour.charAt(x), Colour.BLACK) << 4 | getColour(textColour.charAt(x), Colour.WHITE));
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            for (var channel : palette.getColour(i)) contents[idx++] = (byte) ((int) (channel * 0xFF) & 0xFF);
        }

        assert idx == contents.length;
        return new TerminalState(colour, width, height, cursorX, cursorY, cursorBlink, cursorColour, cursorBackgroundColour, contents);
    }

    synchronized void read(TerminalState state) {
        resize(state.width, state.height);
        cursorX = state.cursorX;
        cursorY = state.cursorY;
        cursorBlink = state.cursorBlink;

        cursorBackgroundColour = state.cursorBgColour;
        this.cursorColour = state.cursorFgColour;

        var contents = state.contents;
        var idx = 0;
        for (var y = 0; y < height; y++) {
            var text = this.text[y];
            var textColour = this.textColour[y];
            var backColour = backgroundColour[y];

            for (var x = 0; x < width; x++) text.setChar(x, (char) (contents[idx++] & 0xFF));
            for (var x = 0; x < width; x++) {
                var colour = contents[idx++];
                backColour.setChar(x, BASE_16.charAt((colour >> 4) & 0xF));
                textColour.setChar(x, BASE_16.charAt(colour & 0xF));
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            var r = (contents[idx++] & 0xFF) / 255.0;
            var g = (contents[idx++] & 0xFF) / 255.0;
            var b = (contents[idx++] & 0xFF) / 255.0;
            palette.setColour(i, r, g, b);
        }

        assert idx == contents.length;
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
        cursorX = nbt.getIntOr("term_cursorX", 0);
        cursorY = nbt.getIntOr("term_cursorY", 0);
        cursorBlink = nbt.getBooleanOr("term_cursorBlink", false);
        cursorColour = nbt.getIntOr("term_textColour", 0);
        cursorBackgroundColour = nbt.getIntOr("term_bgColour", 0);

        for (var n = 0; n < height; n++) {
            text[n].fill(' ');
            if (nbt.contains("term_text_" + n)) {
                text[n].write(nbt.getStringOr("term_text_" + n, ""));
            }
            textColour[n].fill(BASE_16.charAt(cursorColour));
            if (nbt.contains("term_textColour_" + n)) {
                textColour[n].write(nbt.getStringOr("term_textColour_" + n, ""));
            }
            backgroundColour[n].fill(BASE_16.charAt(cursorBackgroundColour));
            if (nbt.contains("term_textBgColour_" + n)) {
                backgroundColour[n].write(nbt.getStringOr("term_textBgColour_" + n, ""));
            }
        }

        if (nbt.contains("term_palette")) {
            var rgb8 = nbt.getIntArray("term_palette").orElse(null);
            if (rgb8 != null && rgb8.length == Palette.PALETTE_SIZE) {
                for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
                    var colours = Palette.decodeRGB8(rgb8[i]);
                    palette.setColour(i, colours[0], colours[1], colours[2]);
                }
            }

        }
        setChanged();
    }
}
