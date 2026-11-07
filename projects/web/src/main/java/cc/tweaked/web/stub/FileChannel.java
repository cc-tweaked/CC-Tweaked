// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.stub;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Stub for {@link java.nio.channels.FileChannel}.
 */
public abstract class FileChannel implements SeekableByteChannel {
    @Override
    public abstract long position() throws IOException;

    @Override
    public abstract FileChannel position(long position) throws IOException;

    public abstract void force(boolean metadata) throws IOException;

    public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;
}
