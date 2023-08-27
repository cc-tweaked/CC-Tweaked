// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

/**
 * A snapshot of a terminal's state.
 * <p>
 * This is somewhat memory inefficient (we build a buffer, only to write it elsewhere), however it means we get a
 * complete and accurate description of a terminal, which avoids a lot of complexities with resizing terminals, dirty
 * states, etc...
 */
public class TerminalState {
    public final boolean colour;

    public final int width;
    public final int height;

    @Nullable
    private final ByteBuf buffer;

    public TerminalState(@Nullable NetworkedTerminal terminal) {
        if (terminal == null) {
            colour = false;
            width = height = 0;
            buffer = null;
        } else {
            colour = terminal.isColour();
            width = terminal.getWidth();
            height = terminal.getHeight();

            var buf = buffer = Unpooled.buffer();
            terminal.write(new FriendlyByteBuf(buf));
        }
    }

    public TerminalState(FriendlyByteBuf buf) {
        colour = buf.readBoolean();

        if (buf.readBoolean()) {
            width = buf.readVarInt();
            height = buf.readVarInt();

            var length = buf.readVarInt();
            buffer = buf.readBytes(length);
        } else {
            width = height = 0;
            buffer = null;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(colour);

        buf.writeBoolean(buffer != null);
        if (buffer != null) {
            buf.writeVarInt(width);
            buf.writeVarInt(height);
            buf.writeVarInt(buffer.readableBytes());
            buf.writeBytes(buffer, buffer.readerIndex(), buffer.readableBytes());
        }
    }

    public boolean hasTerminal() {
        return buffer != null;
    }

    public int size() {
        return buffer == null ? 0 : buffer.readableBytes();
    }

    public void apply(NetworkedTerminal terminal) {
        if (buffer == null) throw new NullPointerException("buffer");
        terminal.resize(width, height);
        terminal.read(new FriendlyByteBuf(buffer));
    }

    public NetworkedTerminal create() {
        if (buffer == null) throw new NullPointerException("Terminal does not exist");
        var terminal = new NetworkedTerminal(width, height, colour);
        terminal.read(new FriendlyByteBuf(buffer));
        return terminal;
    }
}
