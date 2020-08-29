/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.annotation.Nonnull;

import com.google.common.collect.ObjectArrays;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ArgumentHelper;
import dan200.computercraft.shared.util.StringUtil;

public class BinaryWritableHandle extends HandleGeneric {
    private static final String[] METHOD_NAMES = new String[] {
        "write",
        "flush",
        "close"
    };
    private static final String[] METHOD_SEEK_NAMES = ObjectArrays.concat(METHOD_NAMES, new String[] {"seek"}, String.class);

    private final WritableByteChannel m_writer;
    private final SeekableByteChannel m_seekable;
    private final ByteBuffer single = ByteBuffer.allocate(1);

    public BinaryWritableHandle(WritableByteChannel channel) {
        this(channel, channel);
    }

    public BinaryWritableHandle(WritableByteChannel channel, Closeable closeable) {
        super(closeable);
        this.m_writer = channel;
        this.m_seekable = asSeekable(channel);
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return this.m_seekable == null ? METHOD_NAMES : METHOD_SEEK_NAMES;
    }

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException {
        switch (method) {
        case 0: // write
            this.checkOpen();
            try {
                if (args.length > 0 && args[0] instanceof Number) {
                    int number = ((Number) args[0]).intValue();
                    this.single.clear();
                    this.single.put((byte) number);
                    this.single.flip();

                    this.m_writer.write(this.single);
                } else if (args.length > 0 && args[0] instanceof String) {
                    String value = (String) args[0];
                    this.m_writer.write(ByteBuffer.wrap(StringUtil.encodeString(value)));
                } else {
                    throw ArgumentHelper.badArgument(0, "string or number", args.length > 0 ? args[0] : null);
                }
                return null;
            } catch (IOException e) {
                throw new LuaException(e.getMessage());
            }
        case 1: // flush
            this.checkOpen();
            try {
                // Technically this is not needed
                if (this.m_writer instanceof FileChannel) {
                    ((FileChannel) this.m_writer).force(false);
                }

                return null;
            } catch (IOException e) {
                return null;
            }
        case 2: // close
            this.close();
            return null;
        case 3: // seek
            this.checkOpen();
            return handleSeek(this.m_seekable, args);
        default:
            return null;
        }
    }
}
