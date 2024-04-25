// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

/**
 * A snapshot of a terminal's state.
 * <p>
 * This is somewhat memory inefficient (we build a buffer, only to write it elsewhere), however it means we get a
 * complete and accurate description of a terminal, which avoids a lot of complexities with resizing terminals, dirty
 * states, etc...
 */
public class TerminalState {
    private final boolean colour;
    private final int width;
    private final int height;
    private final ByteBuf buffer;

    public TerminalState(NetworkedTerminal terminal) {
        colour = terminal.isColour();
        width = terminal.getWidth();
        height = terminal.getHeight();

        var buf = buffer = Unpooled.buffer();
        terminal.write(new FriendlyByteBuf(buf));
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable TerminalState create(@Nullable NetworkedTerminal terminal) {
        return terminal == null ? null : new TerminalState(terminal);
    }

    public TerminalState(FriendlyByteBuf buf) {
        colour = buf.readBoolean();
        width = buf.readVarInt();
        height = buf.readVarInt();

        var length = buf.readVarInt();
        buffer = buf.readBytes(length);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(colour);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeVarInt(buffer.readableBytes());
        buf.writeBytes(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    public int size() {
        return buffer.readableBytes();
    }

    public void apply(NetworkedTerminal terminal) {
        terminal.resize(width, height);
        terminal.read(new FriendlyByteBuf(buffer));
    }

    public NetworkedTerminal create() {
        var terminal = new NetworkedTerminal(width, height, colour);
        terminal.read(new FriendlyByteBuf(buffer));
        return terminal;
    }
}
