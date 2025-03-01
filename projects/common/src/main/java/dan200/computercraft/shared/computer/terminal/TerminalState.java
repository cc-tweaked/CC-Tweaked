// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * A snapshot of a terminal's state.
 * <p>
 * This is somewhat memory inefficient (we build a buffer, only to write it elsewhere), however it means we get a
 * complete and accurate description of a terminal, which avoids a lot of complexities with resizing terminals, dirty
 * states, etc...
 */
public class TerminalState {
    public static final StreamCodec<FriendlyByteBuf, TerminalState> STREAM_CODEC = StreamCodec.ofMember(TerminalState::write, TerminalState::new);

    private final boolean colour;
    final int width;
    final int height;
    final int cursorX;
    final int cursorY;
    final boolean cursorBlink;
    final int cursorBgColour;
    final int cursorFgColour;
    final byte[] contents;

    TerminalState(
        boolean colour, int width, int height, int cursorX, int cursorY, boolean cursorBlink, int cursorFgColour, int cursorBgColour, byte[] contents
    ) {
        this.colour = colour;
        this.width = width;
        this.height = height;
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.cursorBlink = cursorBlink;
        this.cursorFgColour = cursorFgColour;
        this.cursorBgColour = cursorBgColour;
        this.contents = contents;
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable TerminalState create(@Nullable NetworkedTerminal terminal) {
        return terminal == null ? null : terminal.write();
    }

    private TerminalState(FriendlyByteBuf buf) {
        colour = buf.readBoolean();
        width = buf.readVarInt();
        height = buf.readVarInt();
        cursorX = buf.readVarInt();
        cursorY = buf.readVarInt();
        cursorBlink = buf.readBoolean();

        var cursorColour = buf.readByte();
        this.cursorBgColour = (cursorColour >> 4) & 0xF;
        this.cursorFgColour = cursorColour & 0xF;

        contents = buf.readByteArray();
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(colour);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeVarInt(cursorX);
        buf.writeVarInt(cursorY);
        buf.writeBoolean(cursorBlink);
        buf.writeByte(cursorBgColour << 4 | cursorFgColour);

        buf.writeByteArray(contents);
    }

    public int size() {
        return contents.length;
    }

    public void apply(NetworkedTerminal terminal) {
        terminal.read(this);
    }

    public NetworkedTerminal create() {
        var terminal = new NetworkedTerminal(width, height, colour);
        terminal.read(this);
        return terminal;
    }
}
