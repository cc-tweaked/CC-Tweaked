// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A seekable, readable byte channel which is backed by a {@link ByteBuffer}.
 */
public class ByteBufferChannel implements SeekableByteChannel {
    private boolean closed = false;
    private int position = 0;

    private final ByteBuffer backing;

    public ByteBufferChannel(ByteBuffer backing) {
        this.backing = backing;
    }

    @Override
    public int read(ByteBuffer destination) throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        Objects.requireNonNull(destination, "destination");

        if (position >= backing.limit()) return -1;

        var remaining = Math.min(backing.limit() - position, destination.remaining());

        var destPos = destination.position();
        destination.put(destPos, backing, position, remaining);
        destination.position(destPos + remaining);
        position += remaining;
        return remaining;
    }

    @Override
    public int write(ByteBuffer src) throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        return backing.limit();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws ClosedChannelException {
        if (closed) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
