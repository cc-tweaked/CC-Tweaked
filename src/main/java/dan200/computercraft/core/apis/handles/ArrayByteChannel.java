/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A seekable, readable byte channel which is backed by a simple byte array.
 */
public class ArrayByteChannel implements SeekableByteChannel {
    private final byte[] backing;
    private boolean closed = false;
    private int position = 0;

    public ArrayByteChannel(byte[] backing) {
        this.backing = backing;
    }

    @Override
    public int read(ByteBuffer destination) throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        Objects.requireNonNull(destination, "destination");

        if (this.position >= this.backing.length) {
            return -1;
        }

        int remaining = Math.min(this.backing.length - this.position, destination.remaining());
        destination.put(this.backing, this.position, remaining);
        this.position += remaining;
        return remaining;
    }

    @Override
    public int write(ByteBuffer src) throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        return this.position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        this.position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        return this.backing.length;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return !this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }
}
