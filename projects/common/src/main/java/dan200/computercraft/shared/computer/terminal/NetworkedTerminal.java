// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.core.terminal.Palette;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Colour;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class NetworkedTerminal extends Terminal {
    public NetworkedTerminal(int width, int height, boolean colour) {
        super(width, height, colour);
    }

    public NetworkedTerminal(int width, int height, boolean colour, Runnable changedCallback) {
        super(width, height, colour, changedCallback);
    }

    public synchronized void write(FriendlyByteBuf buffer) {
        buffer.writeInt(cursorX);
        buffer.writeInt(cursorY);
        buffer.writeBoolean(cursorBlink);
        buffer.writeByte(cursorBackgroundColour << 4 | cursorColour);

        for (var y = 0; y < height; y++) {
            var text = this.text[y];
            var textColour = this.textColour[y];
            var backColour = backgroundColour[y];

            for (var x = 0; x < width; x++) buffer.writeByte(text.charAt(x) & 0xFF);
            for (var x = 0; x < width; x++) {
                buffer.writeByte(getColour(
                    backColour.charAt(x), Colour.BLACK) << 4 |
                    getColour(textColour.charAt(x), Colour.WHITE)
                );
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            for (var channel : palette.getColour(i)) buffer.writeByte((int) (channel * 0xFF) & 0xFF);
        }
    }

    public synchronized void read(FriendlyByteBuf buffer) {
        cursorX = buffer.readInt();
        cursorY = buffer.readInt();
        cursorBlink = buffer.readBoolean();

        var cursorColour = buffer.readByte();
        cursorBackgroundColour = (cursorColour >> 4) & 0xF;
        this.cursorColour = cursorColour & 0xF;

        for (var y = 0; y < height; y++) {
            var text = this.text[y];
            var textColour = this.textColour[y];
            var backColour = backgroundColour[y];

            for (var x = 0; x < width; x++) text.setChar(x, (char) (buffer.readByte() & 0xFF));
            for (var x = 0; x < width; x++) {
                var colour = buffer.readByte();
                backColour.setChar(x, BASE_16.charAt((colour >> 4) & 0xF));
                textColour.setChar(x, BASE_16.charAt(colour & 0xF));
            }
        }

        for (var i = 0; i < Palette.PALETTE_SIZE; i++) {
            var r = (buffer.readByte() & 0xFF) / 255.0;
            var g = (buffer.readByte() & 0xFF) / 255.0;
            var b = (buffer.readByte() & 0xFF) / 255.0;
            palette.setColour(i, r, g, b);
        }
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
