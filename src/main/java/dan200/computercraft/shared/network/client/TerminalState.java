/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.IoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;

/**
 * A snapshot of a terminal's state.
 *
 * This is somewhat memory inefficient (we build a buffer, only to write it elsewhere), however it means we get a complete and accurate description of a
 * terminal, which avoids a lot of complexities with resizing terminals, dirty states, etc...
 */
public class TerminalState {
    public final boolean colour;

    public final int width;
    public final int height;

    private final boolean compress;

    @Nullable private final ByteBuf buffer;

    private ByteBuf compressed;

    public TerminalState(boolean colour, @Nullable Terminal terminal) {
        this(colour, terminal, true);
    }

    public TerminalState(boolean colour, @Nullable Terminal terminal, boolean compress) {
        this.colour = colour;
        this.compress = compress;

        if (terminal == null) {
            this.width = this.height = 0;
            this.buffer = null;
        } else {
            this.width = terminal.getWidth();
            this.height = terminal.getHeight();

            ByteBuf buf = this.buffer = Unpooled.buffer();
            terminal.write(new PacketByteBuf(buf));
        }
    }

    public TerminalState(PacketByteBuf buf) {
        this.colour = buf.readBoolean();
        this.compress = buf.readBoolean();

        if (buf.readBoolean()) {
            this.width = buf.readVarInt();
            this.height = buf.readVarInt();

            int length = buf.readVarInt();
            this.buffer = readCompressed(buf, length, this.compress);
        } else {
            this.width = this.height = 0;
            this.buffer = null;
        }
    }

    private static ByteBuf readCompressed(ByteBuf buf, int length, boolean compress) {
        if (compress) {
            ByteBuf buffer = Unpooled.buffer();
            InputStream stream = null;
            try {
                stream = new GZIPInputStream(new ByteBufInputStream(buf, length));
                byte[] swap = new byte[8192];
                while (true) {
                    int bytes = stream.read(swap);
                    if (bytes == -1) {
                        break;
                    }
                    buffer.writeBytes(swap, 0, bytes);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IoUtil.closeQuietly(stream);
            }
            return buffer;
        } else {
            ByteBuf buffer = Unpooled.buffer(length);
            buf.readBytes(buffer, length);
            return buffer;
        }
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.colour);
        buf.writeBoolean(this.compress);

        buf.writeBoolean(this.buffer != null);
        if (this.buffer != null) {
            buf.writeVarInt(this.width);
            buf.writeVarInt(this.height);

            ByteBuf sendBuffer = this.getCompressed();
            buf.writeVarInt(sendBuffer.readableBytes());
            buf.writeBytes(sendBuffer, sendBuffer.readerIndex(), sendBuffer.readableBytes());
        }
    }

    private ByteBuf getCompressed() {
        if (this.buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (!this.compress) {
            return this.buffer;
        }
        if (this.compressed != null) {
            return this.compressed;
        }

        ByteBuf compressed = Unpooled.directBuffer();
        OutputStream stream = null;
        try {
            stream = new GZIPOutputStream(new ByteBufOutputStream(compressed));
            stream.write(this.buffer.array(), this.buffer.arrayOffset(), this.buffer.readableBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IoUtil.closeQuietly(stream);
        }

        return this.compressed = compressed;
    }

    public boolean hasTerminal() {
        return this.buffer != null;
    }

    public int size() {
        return this.buffer == null ? 0 : this.buffer.readableBytes();
    }

    public void apply(Terminal terminal) {
        if (this.buffer == null) {
            throw new NullPointerException("buffer");
        }
        terminal.read(new PacketByteBuf(this.buffer));
    }
}
