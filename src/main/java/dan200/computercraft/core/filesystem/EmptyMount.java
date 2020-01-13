/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IMount;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

public class EmptyMount implements IMount
{
    @Override
    public boolean exists( @Nonnull String path )
    {
        return path.isEmpty();
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        return path.isEmpty();
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents )
    {
    }

    @Override
    public long getSize( @Nonnull String path )
    {
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead( @Nonnull String path ) throws IOException
    {
        throw new FileOperationException( path, "No such file" );
    }

    @Nonnull
    @Override
    @Deprecated
    public ReadableByteChannel openChannelForRead( @Nonnull String path ) throws IOException
    {
        throw new FileOperationException( path, "No such file" );
    }
}
