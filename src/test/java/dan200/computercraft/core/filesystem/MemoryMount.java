/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IWritableMount;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/**
 * Mounts in memory
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
    @Deprecated
    public OutputStream openForWrite( @Nonnull final String path )
    {
        return new ByteArrayOutputStream()
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                files.put( path, toByteArray() );
            }
        };
    }

    @Nonnull
    @Override
    @Deprecated
    public OutputStream openForAppend( @Nonnull final String path ) throws IOException
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

        return stream;
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
            if( file.startsWith( path ) ) files.add( file );
        }
    }

    @Override
    public long getSize( @Nonnull String path )
    {
        throw new RuntimeException( "Not implemented" );
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead( @Nonnull String path )
    {
        return new ByteArrayInputStream( files.get( path ) );
    }

    public MemoryMount addFile( String file, String contents )
    {
        files.put( file, contents.getBytes() );
        return this;
    }
}
