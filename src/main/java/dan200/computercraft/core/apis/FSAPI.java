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

/**
 * The FS API allows you to manipulate files and the filesystem.
 *
 * @cc.module fs
 */
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

    /**
     * Returns a list of files in a directory.
     *
     * @param path The path to list.
     * @return A table with a list of files in the directory.
     * @throws LuaException If the path doesn't exist.
     */
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

    /**
     * Combines two parts of a path into one full path, adding separators as
     * needed.
     *
     * @param pathA The first part of the path. For example, a parent directory path.
     * @param pathB The second part of the path. For example, a file name.
     * @return The new path, with separators added between parts as needed.
     */
    @LuaFunction
    public final String combine( String pathA, String pathB )
    {
        return fileSystem.combine( pathA, pathB );
    }

    /**
     * Returns the file name portion of a path.
     *
     * @param path The path to get the name from.
     * @return The final part of the path (the file name).
     */
    @LuaFunction
    public final String getName( String path )
    {
        return FileSystem.getName( path );
    }

    /**
     * Returns the parent directory portion of a path.
     *
     * @param path The path to get the directory from.
     * @return The path with the final part removed (the parent directory).
     */
    @LuaFunction
    public final String getDir( String path )
    {
        return FileSystem.getDirectory( path );
    }

    /**
     * Returns the size of the specified file.
     *
     * @param path The file to get the file size of.
     * @return The size of the file, in bytes.
     * @throws LuaException If the path doesn't exist.
     */
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

    /**
     * Returns whether the specified path exists.
     *
     * @param path The path to check the existence of.
     * @return Whether the path exists.
     */
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

    /**
     * Returns whether the specified path is a directory.
     *
     * @param path The path to check.
     * @return Whether the path is a directory.
     */
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

    /**
     * Returns whether a path is read-only.
     *
     * @param path The path to check.
     * @return Whether the path cannot be written to.
     */
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

    /**
     * Creates a directory, and any missing parents, at the specified path.
     *
     * @param path The path to the directory to create.
     * @throws LuaException If the directory couldn't be created.
     */
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

    /**
     * Moves a file or directory from one path to another.
     *
     * Any parent directories are created as needed.
     *
     * @param path The current file or directory to move from.
     * @param dest The destination path for the file or directory.
     * @throws LuaException If the file or directory couldn't be moved.
     */
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

    /**
     * Copies a file or directory to a new path.
     *
     * Any parent directories are created as needed.
     *
     * @param path The file or directory to copy.
     * @param dest The path to the destination file or directory.
     * @throws LuaException If the file or directory couldn't be copied.
     */
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

    /**
     * Deletes a file or directory.
     *
     * If the path points to a directory, all of the enclosed files and
     * subdirectories are also deleted.
     *
     * @param path The path to the file or directory to delete.
     * @throws LuaException If the file or directory couldn't be deleted.
     */
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

    /**
     * Opens a file for reading or writing at a path.
     *
     * The mode parameter can be {@code r} to read, {@code w} to write (deleting
     * all contents), or {@code a} to append (keeping contents). If {@code b} is
     * added to the end, the file will be opened in binary mode; otherwise, it's
     * opened in text mode.
     *
     * @param path The path to the file to open.
     * @param mode The mode to open the file with.
     * @return A file handle object for the file, or {@code nil} + an error message on error.
     * @throws LuaException If an invalid mode was specified.
     */
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

    /**
     * Returns the name of the mount that the specified path is located on.
     *
     * @param path The path to get the drive of.
     * @return The name of the drive that the file is on; e.g. {@code hdd} for local files, or {@code rom} for ROM files.
     * @throws LuaException If the path doesn't exist.
     */
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

    /**
     * Returns the amount of free space available on the drive the path is
     * located on.
     *
     * @param path The path to check the free space for.
     * @return The amount of free space available, in bytes.
     * @throws LuaException If the path doesn't exist.
     */
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

    /**
     * Searches for files matching a string with wildcards.
     *
     * This string is formatted like a normal path string, but can include any
     * number of wildcards ({@code *}) to look for files matching anything.
     * For example, {@code rom/* /command*} will look for any path starting with
     * {@code command} inside any subdirectory of {@code /rom}.
     *
     * @param path The wildcard-qualified path to search for.
     * @return A list of paths that match the search string.
     * @throws LuaException If the path doesn't exist.
     */
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

    /**
     * Returns true if a path is mounted to the parent filesystem.
     *
     * The root filesystem "/" is considered a mount, along with disk folders and the rom folder. Other programs
     * (such as network shares) can extend this to make other mount types by correctly assigning their return value for
     * getDrive.
     *
     * @param path The path of the drive to get.
     * @return The drive's capacity.
     * @throws LuaException If the capacity cannot be determined.
     * @cc.treturn number|nil This drive's capacity. This will be nil for "read-only" drives, such as the ROM or
     * treasure disks.
     */
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

    /**
     * Get attributes about a specific file or folder.
     *
     * The returned attributes table contains information about the size of the file, whether it is a directory, and
     * when it was created and last modified.
     *
     * The creation and modification times are given as the number of milliseconds since the UNIX epoch. This may be
     * given to {@link OSAPI#date} in order to convert it to more usable form.
     *
     * @param path The path to get attributes for.
     * @return The resulting attributes.
     * @throws LuaException If the path does not exist.
     * @cc.treturn { size = number, isDir = boolean, created = number, modified = number } The resulting attributes.
     * @see #getSize If you only care about the file's size.
     * @see #isDir If you only care whether a path is a directory or not.
     */
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
