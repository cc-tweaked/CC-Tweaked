/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IFileSystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class FileSystemWrapperMount implements IFileSystem
{
    private final FileSystem filesystem;

    public FileSystemWrapperMount( FileSystem filesystem )
    {
        this.filesystem = filesystem;
    }

    @Override
    public void makeDirectory( @Nonnull String path ) throws IOException
    {
        try
        {
            filesystem.makeDir( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void delete( @Nonnull String path ) throws IOException
    {
        try
        {
            filesystem.delete( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead( @Nonnull String path ) throws IOException
    {
        try
        {
            // FIXME: Think of a better way of implementing this, so closing this will close on the computer.
            return filesystem.openForRead( path, Function.identity() ).get();
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openForWrite( @Nonnull String path ) throws IOException
    {
        try
        {
            return filesystem.openForWrite( path, false, Function.identity() ).get();
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openForAppend( @Nonnull String path ) throws IOException
    {
        try
        {
            return filesystem.openForWrite( path, true, Function.identity() ).get();
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public long getRemainingSpace() throws IOException
    {
        try
        {
            return filesystem.getFreeSpace( "/" );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public boolean exists( @Nonnull String path ) throws IOException
    {
        try
        {
            return filesystem.exists( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public boolean isDirectory( @Nonnull String path ) throws IOException
    {
        try
        {
            return filesystem.isDir( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        try
        {
            Collections.addAll( contents, filesystem.list( path ) );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        try
        {
            return filesystem.getSize( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public String combine( String path, String child )
    {
        return filesystem.combine( path, child );
    }

    @Override
    public void copy( String from, String to ) throws IOException
    {
        try
        {
            filesystem.copy( from, to );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void move( String from, String to ) throws IOException
    {
        try
        {
            filesystem.move( from, to );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }
}
