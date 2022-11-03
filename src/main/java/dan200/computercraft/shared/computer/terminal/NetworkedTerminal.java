/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Colour;
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

        palette.write(buffer);
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

        palette.read(buffer);
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

        palette.writeToNBT(nbt);
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

        palette.readFromNBT(nbt);
        setChanged();
    }
}
