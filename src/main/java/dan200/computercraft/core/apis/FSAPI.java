/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
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
 * The FS API provides access to the computer's files and filesystem, allowing you to manipulate files, directories and
 * paths. This includes:
 *
 * <ul>
 * <li>**Reading and writing files:** Call {@link #open} to obtain a file "handle", which can be used to read from or
 * write to a file.</li>
 * <li>**Path manipulation:** {@link #combine}, {@link #getName} and {@link #getDir} allow you to manipulate file
 * paths, joining them together or extracting components.</li>
 * <li>**Querying paths:** For instance, checking if a file exists, or whether it's a directory. See {@link #getSize},
 * {@link #exists}, {@link #isDir}, {@link #isReadOnly} and {@link #attributes}.</li>
 * <li>**File and directory manipulation:** For instance, moving or copying files. See {@link #makeDir}, {@link #move},
 * {@link #copy} and {@link #delete}.</li>
 * </ul>
 *
 * :::note
 * All functions in the API work on absolute paths, and do not take the @{shell.dir|current directory} into account.
 * You can use @{shell.resolve} to convert a relative path into an absolute one.
 * :::
 *
 * ## Mounts
 * While a computer can only have one hard drive and filesystem, other filesystems may be "mounted" inside it. For
 * instance, the {@link dan200.computercraft.shared.peripheral.diskdrive.DiskDrivePeripheral drive peripheral} mounts
 * its disk's contents at {@code "disk/"}, {@code "disk1/"}, etc...
 *
 * You can see which mount a path belongs to with the {@link #getDrive} function. This returns {@code "hdd"} for the
 * computer's main filesystem ({@code "/"}), {@code "rom"} for the rom ({@code "rom/"}).
 *
 * Most filesystems have a limited capacity, operations which would cause that capacity to be reached (such as writing
 * an incredibly large file) will fail. You can see a mount's capacity with {@link #getCapacity} and the remaining
 * space with {@link #getFreeSpace}.
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
     * @cc.usage List all files under {@code /rom/}
     * <pre>{@code
     * local files = fs.list("/rom/")
     * for i = 1, #files do
     *   print(files[i])
     * end
     * }</pre>
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
     * Combines several parts of a path into one full path, adding separators as
     * needed.
     *
     * @param arguments The paths to combine.
     * @return The new path, with separators added between parts as needed.
     * @throws LuaException On argument errors.
     * @cc.tparam string path The first part of the path. For example, a parent directory path.
     * @cc.tparam string ... Additional parts of the path to combine.
     * @cc.changed 1.95.0 Now supports multiple arguments.
     * @cc.usage Combine several file paths together
     * <pre>{@code
     * fs.combine("/rom/programs", "../apis", "parallel.lua")
     * -- => rom/apis/parallel.lua
     * }</pre>
     */
    @LuaFunction
    public final String combine( IArguments arguments ) throws LuaException
    {
        StringBuilder result = new StringBuilder();
        result.append( FileSystem.sanitizePath( arguments.getString( 0 ), true ) );

        for( int i = 1, n = arguments.count(); i < n; i++ )
        {
            String part = FileSystem.sanitizePath( arguments.getString( i ), true );
            if( result.length() != 0 && !part.isEmpty() ) result.append( '/' );
            result.append( part );
        }

        return FileSystem.sanitizePath( result.toString(), true );
    }

    /**
     * Returns the file name portion of a path.
     *
     * @param path The path to get the name from.
     * @return The final part of the path (the file name).
     * @cc.since 1.2
     * @cc.usage Get the file name of {@code rom/startup.lua}
     * <pre>{@code
     * fs.getName("rom/startup.lua")
     * -- => startup.lua
     * }</pre>
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
     * @cc.since 1.63
     * @cc.usage Get the directory name of {@code rom/startup.lua}
     * <pre>{@code
     * fs.getDir("rom/startup.lua")
     * -- => rom
     * }</pre>
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
     * @cc.since 1.3
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

    // FIXME: Add individual handle type documentation

    /**
     * Opens a file for reading or writing at a path.
     *
     * The {@code mode} string can be any of the following:
     * <ul>
     * <li><strong>"r"</strong>: Read mode</li>
     * <li><strong>"w"</strong>: Write mode</li>
     * <li><strong>"a"</strong>: Append mode</li>
     * </ul>
     *
     * The mode may also have a "b" at the end, which opens the file in "binary
     * mode". This allows you to read binary files, as well as seek within a file.
     *
     * @param path The path to the file to open.
     * @param mode The mode to open the file with.
     * @return A file handle object for the file, or {@code nil} + an error message on error.
     * @throws LuaException If an invalid mode was specified.
     * @cc.treturn [1] table A file handle object for the file.
     * @cc.treturn [2] nil If the file does not exist, or cannot be opened.
     * @cc.treturn string|nil A message explaining why the file cannot be opened.
     * @cc.usage Read the contents of a file.
     * <pre>{@code
     * local file = fs.open("/rom/help/intro.txt", "r")
     * local contents = file.readAll()
     * file.close()
     *
     * print(contents)
     * }</pre>
     * @cc.usage Open a file and read all lines into a table. @{io.lines} offers an alternative way to do this.
     * <pre>{@code
     * local file = fs.open("/rom/motd.txt", "r")
     * local lines = {}
     * while true do
     *   local line = file.readLine()
     *
     *   -- If line is nil then we've reached the end of the file and should stop
     *   if not line then break end
     *
     *   lines[#lines + 1] = line
     * end
     *
     * file.close()
     *
     * print(lines[math.random(#lines)]) -- Pick a random line and print it.
     * }</pre>
     * @cc.usage Open a file and write some text to it. You can run {@code edit out.txt} to see the written text.
     * <pre>{@code
     * local file = fs.open("out.txt", "w")
     * file.write("Just testing some code")
     * file.close() -- Remember to call close, otherwise changes may not be written!
     * }</pre>
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
     * @cc.treturn string The name of the drive that the file is on; e.g. {@code hdd} for local files, or {@code rom} for ROM files.
     * @cc.usage Print the drives of a couple of mounts:
     *
     * <pre>{@code
     * print("/: " .. fs.getDrive("/"))
     * print("/rom/: " .. fs.getDrive("rom"))
     * }</pre>
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
     * @cc.treturn number|"unlimited" The amount of free space available, in bytes, or "unlimited".
     * @cc.since 1.4
     * @see #getCapacity To get the capacity of this drive.
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
     * For example, <code>rom/&#42;/command*</code> will look for any path starting with
     * {@code command} inside any subdirectory of {@code /rom}.
     *
     * @param path The wildcard-qualified path to search for.
     * @return A list of paths that match the search string.
     * @throws LuaException If the path doesn't exist.
     * @cc.since 1.6
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
     * Returns the capacity of the drive the path is located on.
     *
     * @param path The path of the drive to get.
     * @return The drive's capacity.
     * @throws LuaException If the capacity cannot be determined.
     * @cc.treturn number|nil This drive's capacity. This will be nil for "read-only" drives, such as the ROM or
     * treasure disks.
     * @cc.since 1.87.0
     * @see #getFreeSpace To get the free space available on this drive.
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
     * The returned attributes table contains information about the size of the file, whether it is a directory,
     * when it was created and last modified, and whether it is read only.
     *
     * The creation and modification times are given as the number of milliseconds since the UNIX epoch. This may be
     * given to {@link OSAPI#date} in order to convert it to more usable form.
     *
     * @param path The path to get attributes for.
     * @return The resulting attributes.
     * @throws LuaException If the path does not exist.
     * @cc.treturn { size = number, isDir = boolean, isReadOnly = boolean, created = number, modified = number } The resulting attributes.
     * @cc.since 1.87.0
     * @cc.changed 1.91.0 Renamed `modification` field to `modified`.
     * @cc.changed 1.95.2 Added `isReadOnly` to attributes.
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
            result.put( "modified", getFileTime( attributes.lastModifiedTime() ) );
            result.put( "created", getFileTime( attributes.creationTime() ) );
            result.put( "size", attributes.isDirectory() ? 0 : attributes.size() );
            result.put( "isDir", attributes.isDirectory() );
            result.put( "isReadOnly", fileSystem.isReadOnly( path ) );
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
