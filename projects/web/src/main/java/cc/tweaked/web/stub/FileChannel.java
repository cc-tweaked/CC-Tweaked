// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.stub;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A stub for {@link java.nio.channels.FileChannel}. This is never constructed, only used in {@code instanceof} checks.
 */
public abstract class FileChannel implements SeekableByteChannel {
    private FileChannel() {
    }

    @Override
    public abstract FileChannel position(long newPosition) throws IOException;

    public abstract void force(boolean metadata) throws IOException;

    public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;
}
