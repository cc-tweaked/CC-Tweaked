/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

public class SubMount implements IMount
{
    private IMount m_parent;
    private String m_subPath;

    public SubMount( IMount parent, String subPath )
    {
        m_parent = parent;
        m_subPath = subPath;
    }

    // IMount implementation

    @Override
    public boolean exists( @Nonnull String path ) throws IOException
    {
        return m_parent.exists( getFullPath( path ) );
    }

    @Override
    public boolean isDirectory( @Nonnull String path ) throws IOException
    {
        return m_parent.isDirectory( getFullPath( path ) );
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        m_parent.list( getFullPath( path ), contents );
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        return m_parent.getSize( getFullPath( path ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead( @Nonnull String path ) throws IOException
    {
        return m_parent.openForRead( getFullPath( path ) );
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead( @Nonnull String path ) throws IOException
    {
        return m_parent.openChannelForRead( getFullPath( path ) );
    }

    private String getFullPath( String path )
    {
        if( path.isEmpty() )
        {
            return m_subPath;
        }
        else
        {
            return m_subPath + "/" + path;
        }
    }
}
