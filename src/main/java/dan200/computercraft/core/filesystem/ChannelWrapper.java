/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;

/**
 * Wraps some closeable object such as a buffered writer, and the underlying stream.
 *
 * When flushing a buffer before closing, some implementations will not close the buffer if an exception is thrown
 * this causes us to release the channel, but not actually close it. This wrapper will attempt to close the wrapper (and
 * so hopefully flush the channel), and then close the underlying channel.
 *
 * @param <T> The type of the closeable object to write.
 */
class ChannelWrapper<T extends Closeable> implements Closeable
{
    private final T wrapper;
    private final Channel channel;

    ChannelWrapper( T wrapper, Channel channel )
    {
        this.wrapper = wrapper;
        this.channel = channel;
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            wrapper.close();
        }
        finally
        {
            channel.close();
        }
    }

    T get()
    {
        return wrapper;
    }
}
