/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

/**
 * In-memory file mounts.
 */
public class MemoryMount implements IWritableMount
{
    private final Map<String, byte[]> files = new HashMap<>();
    private final Set<String> directories = new HashSet<>();

    public MemoryMount()
    {
        directories.add( "" );
    }


    @Override
    public void makeDirectory( @Nonnull String path )
    {
        File file = new File( path );
        while( file != null )
        {
            directories.add( file.getPath() );
            file = file.getParentFile();
        }
    }

    @Override
    public void delete( @Nonnull String path )
    {
        if( files.containsKey( path ) )
        {
            files.remove( path );
        }
        else
        {
            directories.remove( path );
            for( String file : files.keySet().toArray( new String[0] ) )
            {
                if( file.startsWith( path ) )
                {
                    files.remove( file );
                }
            }

            File parent = new File( path ).getParentFile();
            if( parent != null ) delete( parent.getPath() );
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openForWrite( @Nonnull final String path )
    {
        return Channels.newChannel( new ByteArrayOutputStream()
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                files.put( path, toByteArray() );
            }
        } );
    }

    @Nonnull
    @Override
    public WritableByteChannel openForAppend( @Nonnull final String path ) throws IOException
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                files.put( path, toByteArray() );
            }
        };

        byte[] current = files.get( path );
        if( current != null ) stream.write( current );

        return Channels.newChannel( stream );
    }

    @Override
    public long getRemainingSpace()
    {
        return 1000000L;
    }

    @Override
    public boolean exists( @Nonnull String path )
    {
        return files.containsKey( path ) || directories.contains( path );
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        return directories.contains( path );
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> files )
    {
        for( String file : this.files.keySet() )
        {
            if( file.startsWith( path ) ) files.add( file.substring( path.length() + 1 ) );
        }
    }

    @Override
    public long getSize( @Nonnull String path )
    {
        throw new RuntimeException( "Not implemented" );
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead( @Nonnull String path )
    {
        return new ArrayByteChannel( files.get( path ) );
    }

    public MemoryMount addFile( String file, String contents )
    {
        files.put( file, contents.getBytes() );
        return this;
    }
}
