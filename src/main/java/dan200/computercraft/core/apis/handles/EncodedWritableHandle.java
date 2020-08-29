/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

public class EncodedWritableHandle extends HandleGeneric {
    private BufferedWriter m_writer;

    public EncodedWritableHandle(@Nonnull BufferedWriter writer) {
        this(writer, writer);
    }

    public EncodedWritableHandle(@Nonnull BufferedWriter writer, @Nonnull Closeable closable) {
        super(closable);
        this.m_writer = writer;
    }

    public static BufferedWriter openUtf8(WritableByteChannel channel) {
        return open(channel, StandardCharsets.UTF_8);
    }

    public static BufferedWriter open(WritableByteChannel channel, Charset charset) {
        // Create a charset encoder with the same properties as StreamEncoder does for
        // OutputStreams: namely, replace everything instead of erroring.
        CharsetEncoder encoder = charset.newEncoder()
                                        .onMalformedInput(CodingErrorAction.REPLACE)
                                        .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedWriter(Channels.newWriter(channel, encoder, -1));
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[] {
            "write",
            "writeLine",
            "flush",
            "close",
            };
    }

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException {
        switch (method) {
        case 0: // write
        {
            this.checkOpen();
            String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
            try {
                this.m_writer.write(text, 0, text.length());
                return null;
            } catch (IOException e) {
                throw new LuaException(e.getMessage());
            }
        }
        case 1: // writeLine
        {
            this.checkOpen();
            String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
            try {
                this.m_writer.write(text, 0, text.length());
                this.m_writer.newLine();
                return null;
            } catch (IOException e) {
                throw new LuaException(e.getMessage());
            }
        }
        case 2: // flush
            this.checkOpen();
            try {
                this.m_writer.flush();
                return null;
            } catch (IOException e) {
                return null;
            }
        case 3: // close
            this.close();
            return null;
        default:
            return null;
        }
    }
}
