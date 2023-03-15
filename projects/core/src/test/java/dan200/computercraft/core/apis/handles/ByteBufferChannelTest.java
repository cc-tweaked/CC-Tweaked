// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
