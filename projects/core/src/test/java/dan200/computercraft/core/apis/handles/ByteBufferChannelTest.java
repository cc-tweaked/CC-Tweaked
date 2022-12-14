/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.test.core.filesystem.ReadableChannelContract;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class ByteBufferChannelTest implements ReadableChannelContract {
    @Override
    public SeekableByteChannel wrap(byte[] contents) {
        return new ByteBufferChannel(ByteBuffer.wrap(contents));
    }
}
