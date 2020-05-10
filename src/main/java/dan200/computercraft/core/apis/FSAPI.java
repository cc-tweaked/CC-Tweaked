/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.BinaryWritableHandle;
import dan200.computercraft.core.apis.handles.EncodedReadableHandle;
import dan200.computercraft.core.apis.handles.EncodedWritableHandle;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.core.tracking.TrackingField;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;

public class FSAPI implements ILuaAPI
{
    private final IAPIEnvironment environment;
    private FileSystem fileSystem = null;

    public FSAPI( IAPIEnvironment env )
    {
        environment = env;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "fs" };
    }

    @Override
    public void startup()
    {
        fileSystem = environment.getFileSystem();
    }

    @Override
    public void shutdown()
    {
        fileSystem = null;
    }

    @LuaFunction
    public final String[] list( String path ) throws LuaException
    {
        environment.addTrackingChange( TrackingField.FS_OPS );
        try
        {
            return fileSystem.list( path );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final String combine( String pathA, String pathB )
    {
        return fileSystem.combine( pathA, pathB );
    }

    @LuaFunction
    public final String getName( String path )
    {
        return FileSystem.getName( path );
    }

    @LuaFunction
    public final String getDir( String path )
    {
        return FileSystem.getDirectory( path );
    }

    @LuaFunction
    public final long getSize( String path ) throws LuaException
    {
        try
        {
            return fileSystem.getSize( path );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final boolean exists( String path )
    {
        try
        {
            return fileSystem.exists( path );
        }
        catch( FileSystemException e )
        {
            return false;
        }
    }

    @LuaFunction
    public final boolean isDir( String path )
    {
        try
        {
            return fileSystem.isDir( path );
        }
        catch( FileSystemException e )
        {
            return false;
        }
    }

    @LuaFunction
    public final boolean isReadOnly( String path )
    {
        try
        {
            return fileSystem.isReadOnly( path );
        }
        catch( FileSystemException e )
        {
            return false;
        }
    }

    @LuaFunction
    public final void makeDir( String path ) throws LuaException
    {
        try
        {
            environment.addTrackingChange( TrackingField.FS_OPS );
            fileSystem.makeDir( path );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final void move( String path, String dest ) throws LuaException
    {
        try
        {
            environment.addTrackingChange( TrackingField.FS_OPS );
            fileSystem.move( path, dest );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final void copy( String path, String dest ) throws LuaException
    {
        try
        {
            environment.addTrackingChange( TrackingField.FS_OPS );
            fileSystem.copy( path, dest );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final void delete( String path ) throws LuaException
    {
        try
        {
            environment.addTrackingChange( TrackingField.FS_OPS );
            fileSystem.delete( path );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final Object[] open( String path, String mode ) throws LuaException
    {
        environment.addTrackingChange( TrackingField.FS_OPS );
        try
        {
            switch( mode )
            {
                case "r":
                {
                    // Open the file for reading, then create a wrapper around the reader
                    FileSystemWrapper<BufferedReader> reader = fileSystem.openForRead( path, EncodedReadableHandle::openUtf8 );
                    return new Object[] { new EncodedReadableHandle( reader.get(), reader ) };
                }
                case "w":
                {
                    // Open the file for writing, then create a wrapper around the writer
                    FileSystemWrapper<BufferedWriter> writer = fileSystem.openForWrite( path, false, EncodedWritableHandle::openUtf8 );
                    return new Object[] { new EncodedWritableHandle( writer.get(), writer ) };
                }
                case "a":
                {
                    // Open the file for appending, then create a wrapper around the writer
                    FileSystemWrapper<BufferedWriter> writer = fileSystem.openForWrite( path, true, EncodedWritableHandle::openUtf8 );
                    return new Object[] { new EncodedWritableHandle( writer.get(), writer ) };
                }
                case "rb":
                {
                    // Open the file for binary reading, then create a wrapper around the reader
                    FileSystemWrapper<ReadableByteChannel> reader = fileSystem.openForRead( path, Function.identity() );
                    return new Object[] { BinaryReadableHandle.of( reader.get(), reader ) };
                }
                case "wb":
                {
                    // Open the file for binary writing, then create a wrapper around the writer
                    FileSystemWrapper<WritableByteChannel> writer = fileSystem.openForWrite( path, false, Function.identity() );
                    return new Object[] { BinaryWritableHandle.of( writer.get(), writer ) };
                }
                case "ab":
                {
                    // Open the file for binary appending, then create a wrapper around the reader
                    FileSystemWrapper<WritableByteChannel> writer = fileSystem.openForWrite( path, true, Function.identity() );
                    return new Object[] { BinaryWritableHandle.of( writer.get(), writer ) };
                }
                default:
                    throw new LuaException( "Unsupported mode" );
            }
        }
        catch( FileSystemException e )
        {
            return new Object[] { null, e.getMessage() };
        }
    }

    @LuaFunction
    public final Object[] getDrive( String path ) throws LuaException
    {
        try
        {
            return fileSystem.exists( path ) ? new Object[] { fileSystem.getMountLabel( path ) } : null;
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final Object getFreeSpace( String path ) throws LuaException
    {
        try
        {
            long freeSpace = fileSystem.getFreeSpace( path );
            return freeSpace >= 0 ? freeSpace : "unlimited";
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final String[] find( String path ) throws LuaException
    {
        try
        {
            environment.addTrackingChange( TrackingField.FS_OPS );
            return fileSystem.find( path );
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final Object getCapacity( String path ) throws LuaException
    {
        try
        {
            OptionalLong capacity = fileSystem.getCapacity( path );
            return capacity.isPresent() ? capacity.getAsLong() : null;
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    @LuaFunction
    public final Map<String, Object> attributes( String path ) throws LuaException
    {
        try
        {
            BasicFileAttributes attributes = fileSystem.getAttributes( path );
            Map<String, Object> result = new HashMap<>();
            result.put( "modification", getFileTime( attributes.lastModifiedTime() ) );
            result.put( "created", getFileTime( attributes.creationTime() ) );
            result.put( "size", attributes.isDirectory() ? 0 : attributes.size() );
            result.put( "isDir", attributes.isDirectory() );
            return result;
        }
        catch( FileSystemException e )
        {
            throw new LuaException( e.getMessage() );
        }
    }

    private static long getFileTime( FileTime time )
    {
        return time == null ? 0 : time.toMillis();
    }
}
