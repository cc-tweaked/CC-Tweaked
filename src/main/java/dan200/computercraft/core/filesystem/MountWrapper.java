/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.OptionalLong;

class MountWrapper
{
    private String label;
    private String location;

    private IMount mount;
    private IWritableMount writableMount;

    MountWrapper( String label, String location, IMount mount )
    {
        this.label = label;
        this.location = location;
        this.mount = mount;
        writableMount = null;
    }

    MountWrapper( String label, String location, IWritableMount mount )
    {
        this( label, location, (IMount) mount );
        writableMount = mount;
    }

    public String getLabel()
    {
        return label;
    }

    public String getLocation()
    {
        return location;
    }

    public long getFreeSpace()
    {
        if( writableMount == null ) return 0;

        try
        {
            return writableMount.getRemainingSpace();
        }
        catch( IOException e )
        {
            return 0;
        }
    }

    public OptionalLong getCapacity()
    {
        return writableMount == null ? OptionalLong.empty() : writableMount.getCapacity();
    }

    public boolean isReadOnly( String path )
    {
        return writableMount == null;
    }

    public boolean exists( String path ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            return mount.exists( path );
        }
        catch( IOException e )
        {
            throw new FileSystemException( e.getMessage() );
        }
    }

    public boolean isDirectory( String path ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            return mount.exists( path ) && mount.isDirectory( path );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public void list( String path, List<String> contents ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            if( !mount.exists( path ) || !mount.isDirectory( path ) )
            {
                throw localExceptionOf( path, "Not a directory" );
            }

            mount.list( path, contents );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public long getSize( String path ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            if( !mount.exists( path ) ) throw localExceptionOf( path, "No such file" );
            return mount.isDirectory( path ) ? 0 : mount.getSize( path );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    @Nonnull
    public BasicFileAttributes getAttributes( String path ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            if( !mount.exists( path ) ) throw localExceptionOf( path, "No such file" );
            return mount.getAttributes( path );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public ReadableByteChannel openForRead( String path ) throws FileSystemException
    {
        path = toLocal( path );
        try
        {
            if( mount.exists( path ) && !mount.isDirectory( path ) )
            {
                return mount.openChannelForRead( path );
            }
            else
            {
                throw localExceptionOf( path, "No such file" );
            }
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public void makeDirectory( String path ) throws FileSystemException
    {
        if( writableMount == null ) throw exceptionOf( path, "Access denied" );

        path = toLocal( path );
        try
        {
            if( mount.exists( path ) )
            {
                if( !mount.isDirectory( path ) ) throw localExceptionOf( path, "File exists" );
            }
            else
            {
                writableMount.makeDirectory( path );
            }
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public void delete( String path ) throws FileSystemException
    {
        if( writableMount == null ) throw exceptionOf( path, "Access denied" );

        try
        {
            path = toLocal( path );
            if( mount.exists( path ) )
            {
                writableMount.delete( path );
            }
        }
        catch( AccessDeniedException e )
        {
            throw new FileSystemException( "Access denied" );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public WritableByteChannel openForWrite( String path ) throws FileSystemException
    {
        if( writableMount == null ) throw exceptionOf( path, "Access denied" );

        path = toLocal( path );
        try
        {
            if( mount.exists( path ) && mount.isDirectory( path ) )
            {
                throw localExceptionOf( path, "Cannot write to directory" );
            }
            else
            {
                if( !path.isEmpty() )
                {
                    String dir = FileSystem.getDirectory( path );
                    if( !dir.isEmpty() && !mount.exists( path ) )
                    {
                        writableMount.makeDirectory( dir );
                    }
                }
                return writableMount.openChannelForWrite( path );
            }
        }
        catch( AccessDeniedException e )
        {
            throw new FileSystemException( "Access denied" );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    public WritableByteChannel openForAppend( String path ) throws FileSystemException
    {
        if( writableMount == null ) throw exceptionOf( path, "Access denied" );

        path = toLocal( path );
        try
        {
            if( !mount.exists( path ) )
            {
                if( !path.isEmpty() )
                {
                    String dir = FileSystem.getDirectory( path );
                    if( !dir.isEmpty() && !mount.exists( path ) )
                    {
                        writableMount.makeDirectory( dir );
                    }
                }
                return writableMount.openChannelForWrite( path );
            }
            else if( mount.isDirectory( path ) )
            {
                throw localExceptionOf( path, "Cannot write to directory" );
            }
            else
            {
                return writableMount.openChannelForAppend( path );
            }
        }
        catch( AccessDeniedException e )
        {
            throw new FileSystemException( "Access denied" );
        }
        catch( IOException e )
        {
            throw localExceptionOf( e );
        }
    }

    private String toLocal( String path )
    {
        return FileSystem.toLocal( path, location );
    }

    private FileSystemException localExceptionOf( IOException e )
    {
        if( !location.isEmpty() && e instanceof FileOperationException )
        {
            FileOperationException ex = (FileOperationException) e;
            if( ex.getFilename() != null ) return localExceptionOf( ex.getFilename(), ex.getMessage() );
        }

        return new FileSystemException( e.getMessage() );
    }

    private FileSystemException localExceptionOf( String path, String message )
    {
        if( !location.isEmpty() ) path = path.isEmpty() ? location : location + "/" + path;
        return exceptionOf( path, message );
    }

    private static FileSystemException exceptionOf( String path, String message )
    {
        return new FileSystemException( "/" + path + ": " + message );
    }
}
