/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.ByteStreams;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.shared.util.IoUtil;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public class FileSystem
{
    /**
     * Maximum depth that {@link #copyRecursive(String, MountWrapper, String, MountWrapper, int)} will descend into.
     *
     * This is a pretty arbitrary value, though hopefully it is large enough that it'll never be normally hit. This
     * exists to prevent it overflowing if it ever gets into an infinite loop.
     */
    private static final int MAX_COPY_DEPTH = 128;

    private final FileSystemWrapperMount wrapper = new FileSystemWrapperMount( this );
    private final Map<String, MountWrapper> mounts = new HashMap<>();

    private final HashMap<WeakReference<FileSystemWrapper<?>>, ChannelWrapper<?>> openFiles = new HashMap<>();
    private final ReferenceQueue<FileSystemWrapper<?>> openFileQueue = new ReferenceQueue<>();

    public FileSystem( String rootLabel, IMount rootMount ) throws FileSystemException
    {
        mount( rootLabel, "", rootMount );
    }

    public FileSystem( String rootLabel, IWritableMount rootMount ) throws FileSystemException
    {
        mountWritable( rootLabel, "", rootMount );
    }

    public void close()
    {
        // Close all dangling open files
        synchronized( openFiles )
        {
            for( Closeable file : openFiles.values() ) IoUtil.closeQuietly( file );
            openFiles.clear();
            while( openFileQueue.poll() != null ) ;
        }
    }

    public synchronized void mount( String label, String location, IMount mount ) throws FileSystemException
    {
        if( mount == null ) throw new NullPointerException();
        location = sanitizePath( location );
        if( location.contains( ".." ) ) throw new FileSystemException( "Cannot mount below the root" );
        mount( new MountWrapper( label, location, mount ) );
    }

    public synchronized void mountWritable( String label, String location, IWritableMount mount ) throws FileSystemException
    {
        if( mount == null )
        {
            throw new NullPointerException();
        }
        location = sanitizePath( location );
        if( location.contains( ".." ) )
        {
            throw new FileSystemException( "Cannot mount below the root" );
        }
        mount( new MountWrapper( label, location, mount ) );
    }

    private synchronized void mount( MountWrapper wrapper )
    {
        String location = wrapper.getLocation();
        mounts.remove( location );
        mounts.put( location, wrapper );
    }

    public synchronized void unmount( String path )
    {
        MountWrapper mount = mounts.remove( sanitizePath( path ) );
        if( mount == null ) return;

        cleanup();

        // Close any files which belong to this mount - don't want people writing to a disk after it's been ejected!
        // There's no point storing a Mount -> Wrapper[] map, as openFiles is small and unmount isn't called very
        // often.
        synchronized( openFiles )
        {
            for( Iterator<WeakReference<FileSystemWrapper<?>>> iterator = openFiles.keySet().iterator(); iterator.hasNext(); )
            {
                WeakReference<FileSystemWrapper<?>> reference = iterator.next();
                FileSystemWrapper<?> wrapper = reference.get();
                if( wrapper == null ) continue;

                if( wrapper.mount == mount )
                {
                    wrapper.closeExternally();
                    iterator.remove();
                }
            }
        }
    }

    public String combine( String path, String childPath )
    {
        path = sanitizePath( path, true );
        childPath = sanitizePath( childPath, true );

        if( path.isEmpty() )
        {
            return childPath;
        }
        else if( childPath.isEmpty() )
        {
            return path;
        }
        else
        {
            return sanitizePath( path + '/' + childPath, true );
        }
    }

    public static String getDirectory( String path )
    {
        path = sanitizePath( path, true );
        if( path.isEmpty() )
        {
            return "..";
        }

        int lastSlash = path.lastIndexOf( '/' );
        if( lastSlash >= 0 )
        {
            return path.substring( 0, lastSlash );
        }
        else
        {
            return "";
        }
    }

    public static String getName( String path )
    {
        path = sanitizePath( path, true );
        if( path.isEmpty() ) return "root";

        int lastSlash = path.lastIndexOf( '/' );
        return lastSlash >= 0 ? path.substring( lastSlash + 1 ) : path;
    }

    public synchronized long getSize( String path ) throws FileSystemException
    {
        return getMount( sanitizePath( path ) ).getSize( sanitizePath( path ) );
    }

    public synchronized BasicFileAttributes getAttributes( String path ) throws FileSystemException
    {
        return getMount( sanitizePath( path ) ).getAttributes( sanitizePath( path ) );
    }

    public synchronized String[] list( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );

        // Gets a list of the files in the mount
        List<String> list = new ArrayList<>();
        mount.list( path, list );

        // Add any mounts that are mounted at this location
        for( MountWrapper otherMount : mounts.values() )
        {
            if( getDirectory( otherMount.getLocation() ).equals( path ) )
            {
                list.add( getName( otherMount.getLocation() ) );
            }
        }

        // Return list
        String[] array = new String[list.size()];
        list.toArray( array );
        Arrays.sort( array );
        return array;
    }

    private void findIn( String dir, List<String> matches, Pattern wildPattern ) throws FileSystemException
    {
        String[] list = list( dir );
        for( String entry : list )
        {
            String entryPath = dir.isEmpty() ? entry : dir + "/" + entry;
            if( wildPattern.matcher( entryPath ).matches() )
            {
                matches.add( entryPath );
            }
            if( isDir( entryPath ) )
            {
                findIn( entryPath, matches, wildPattern );
            }
        }
    }

    public synchronized String[] find( String wildPath ) throws FileSystemException
    {
        // Match all the files on the system
        wildPath = sanitizePath( wildPath, true );

        // If we don't have a wildcard at all just check the file exists
        int starIndex = wildPath.indexOf( '*' );
        if( starIndex == -1 )
        {
            return exists( wildPath ) ? new String[] { wildPath } : new String[0];
        }

        // Find the all non-wildcarded directories. For instance foo/bar/baz* -> foo/bar
        int prevDir = wildPath.substring( 0, starIndex ).lastIndexOf( '/' );
        String startDir = prevDir == -1 ? "" : wildPath.substring( 0, prevDir );

        // If this isn't a directory then just abort
        if( !isDir( startDir ) ) return new String[0];

        // Scan as normal, starting from this directory
        Pattern wildPattern = Pattern.compile( "^\\Q" + wildPath.replaceAll( "\\*", "\\\\E[^\\\\/]*\\\\Q" ) + "\\E$" );
        List<String> matches = new ArrayList<>();
        findIn( startDir, matches, wildPattern );

        // Return matches
        String[] array = new String[matches.size()];
        matches.toArray( array );
        return array;
    }

    public synchronized boolean exists( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.exists( path );
    }

    public synchronized boolean isDir( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.isDirectory( path );
    }

    public synchronized boolean isReadOnly( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.isReadOnly( path );
    }

    public synchronized String getMountLabel( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.getLabel();
    }

    public synchronized void makeDir( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        mount.makeDirectory( path );
    }

    public synchronized void delete( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        mount.delete( path );
    }

    public synchronized void move( String sourcePath, String destPath ) throws FileSystemException
    {
        sourcePath = sanitizePath( sourcePath );
        destPath = sanitizePath( destPath );
        if( isReadOnly( sourcePath ) || isReadOnly( destPath ) )
        {
            throw new FileSystemException( "Access denied" );
        }
        if( !exists( sourcePath ) )
        {
            throw new FileSystemException( "No such file" );
        }
        if( exists( destPath ) )
        {
            throw new FileSystemException( "File exists" );
        }
        if( contains( sourcePath, destPath ) )
        {
            throw new FileSystemException( "Can't move a directory inside itself" );
        }
        copy( sourcePath, destPath );
        delete( sourcePath );
    }

    public synchronized void copy( String sourcePath, String destPath ) throws FileSystemException
    {
        sourcePath = sanitizePath( sourcePath );
        destPath = sanitizePath( destPath );
        if( isReadOnly( destPath ) )
        {
            throw new FileSystemException( "/" + destPath + ": Access denied" );
        }
        if( !exists( sourcePath ) )
        {
            throw new FileSystemException( "/" + sourcePath + ": No such file" );
        }
        if( exists( destPath ) )
        {
            throw new FileSystemException( "/" + destPath + ": File exists" );
        }
        if( contains( sourcePath, destPath ) )
        {
            throw new FileSystemException( "/" + sourcePath + ": Can't copy a directory inside itself" );
        }
        copyRecursive( sourcePath, getMount( sourcePath ), destPath, getMount( destPath ), 0 );
    }

    private synchronized void copyRecursive( String sourcePath, MountWrapper sourceMount, String destinationPath, MountWrapper destinationMount, int depth ) throws FileSystemException
    {
        if( !sourceMount.exists( sourcePath ) ) return;
        if( depth >= MAX_COPY_DEPTH ) throw new FileSystemException( "Too many directories to copy" );

        if( sourceMount.isDirectory( sourcePath ) )
        {
            // Copy a directory:
            // Make the new directory
            destinationMount.makeDirectory( destinationPath );

            // Copy the source contents into it
            List<String> sourceChildren = new ArrayList<>();
            sourceMount.list( sourcePath, sourceChildren );
            for( String child : sourceChildren )
            {
                copyRecursive(
                    combine( sourcePath, child ), sourceMount,
                    combine( destinationPath, child ), destinationMount,
                    depth + 1
                );
            }
        }
        else
        {
            // Copy a file:
            try( ReadableByteChannel source = sourceMount.openForRead( sourcePath );
                 WritableByteChannel destination = destinationMount.openForWrite( destinationPath ) )
            {
                // Copy bytes as fast as we can
                ByteStreams.copy( source, destination );
            }
            catch( AccessDeniedException e )
            {
                throw new FileSystemException( "Access denied" );
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
    }

    private void cleanup()
    {
        synchronized( openFiles )
        {
            Reference<?> ref;
            while( (ref = openFileQueue.poll()) != null )
            {
                IoUtil.closeQuietly( openFiles.remove( ref ) );
            }
        }
    }

    private synchronized <T extends Closeable> FileSystemWrapper<T> openFile( @Nonnull MountWrapper mount, @Nonnull Channel channel, @Nonnull T file ) throws FileSystemException
    {
        synchronized( openFiles )
        {
            if( ComputerCraft.maximumFilesOpen > 0 &&
                openFiles.size() >= ComputerCraft.maximumFilesOpen )
            {
                IoUtil.closeQuietly( file );
                IoUtil.closeQuietly( channel );
                throw new FileSystemException( "Too many files already open" );
            }

            ChannelWrapper<T> channelWrapper = new ChannelWrapper<>( file, channel );
            FileSystemWrapper<T> fsWrapper = new FileSystemWrapper<>( this, mount, channelWrapper, openFileQueue );
            openFiles.put( fsWrapper.self, channelWrapper );
            return fsWrapper;
        }
    }

    void removeFile( FileSystemWrapper<?> handle )
    {
        synchronized( openFiles )
        {
            openFiles.remove( handle.self );
        }
    }

    public synchronized <T extends Closeable> FileSystemWrapper<T> openForRead( String path, Function<ReadableByteChannel, T> open ) throws FileSystemException
    {
        cleanup();

        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        ReadableByteChannel channel = mount.openForRead( path );
        return channel != null ? openFile( mount, channel, open.apply( channel ) ) : null;
    }

    public synchronized <T extends Closeable> FileSystemWrapper<T> openForWrite( String path, boolean append, Function<WritableByteChannel, T> open ) throws FileSystemException
    {
        cleanup();

        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        WritableByteChannel channel = append ? mount.openForAppend( path ) : mount.openForWrite( path );
        return channel != null ? openFile( mount, channel, open.apply( channel ) ) : null;
    }

    public synchronized long getFreeSpace( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.getFreeSpace();
    }

    @Nonnull
    public synchronized OptionalLong getCapacity( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.getCapacity();
    }

    private synchronized MountWrapper getMount( String path ) throws FileSystemException
    {
        // Return the deepest mount that contains a given path
        Iterator<MountWrapper> it = mounts.values().iterator();
        MountWrapper match = null;
        int matchLength = 999;
        while( it.hasNext() )
        {
            MountWrapper mount = it.next();
            if( contains( mount.getLocation(), path ) )
            {
                int len = toLocal( path, mount.getLocation() ).length();
                if( match == null || len < matchLength )
                {
                    match = mount;
                    matchLength = len;
                }
            }
        }
        if( match == null )
        {
            throw new FileSystemException( "/" + path + ": Invalid Path" );
        }
        return match;
    }

    public IFileSystem getMountWrapper()
    {
        return wrapper;
    }

    private static String sanitizePath( String path )
    {
        return sanitizePath( path, false );
    }

    private static final Pattern threeDotsPattern = Pattern.compile( "^\\.{3,}$" );

    public static String sanitizePath( String path, boolean allowWildcards )
    {
        // Allow windowsy slashes
        path = path.replace( '\\', '/' );

        // Clean the path or illegal characters.
        final char[] specialChars = new char[] {
            '"', ':', '<', '>', '?', '|', // Sorted by ascii value (important)
        };

        StringBuilder cleanName = new StringBuilder();
        for( int i = 0; i < path.length(); i++ )
        {
            char c = path.charAt( i );
            if( c >= 32 && Arrays.binarySearch( specialChars, c ) < 0 && (allowWildcards || c != '*') )
            {
                cleanName.append( c );
            }
        }
        path = cleanName.toString();

        // Collapse the string into its component parts, removing ..'s
        String[] parts = path.split( "/" );
        Stack<String> outputParts = new Stack<>();
        for( String part : parts )
        {
            if( part.isEmpty() || part.equals( "." ) || threeDotsPattern.matcher( part ).matches() )
            {
                // . is redundant
                // ... and more are treated as .
                continue;
            }

            if( part.equals( ".." ) )
            {
                // .. can cancel out the last folder entered
                if( !outputParts.empty() )
                {
                    String top = outputParts.peek();
                    if( !top.equals( ".." ) )
                    {
                        outputParts.pop();
                    }
                    else
                    {
                        outputParts.push( ".." );
                    }
                }
                else
                {
                    outputParts.push( ".." );
                }
            }
            else if( part.length() >= 255 )
            {
                // If part length > 255 and it is the last part
                outputParts.push( part.substring( 0, 255 ) );
            }
            else
            {
                // Anything else we add to the stack
                outputParts.push( part );
            }
        }

        // Recombine the output parts into a new string
        StringBuilder result = new StringBuilder();
        Iterator<String> it = outputParts.iterator();
        while( it.hasNext() )
        {
            String part = it.next();
            result.append( part );
            if( it.hasNext() )
            {
                result.append( '/' );
            }
        }

        return result.toString();
    }

    public static boolean contains( String pathA, String pathB )
    {
        pathA = sanitizePath( pathA ).toLowerCase( Locale.ROOT );
        pathB = sanitizePath( pathB ).toLowerCase( Locale.ROOT );

        if( pathB.equals( ".." ) )
        {
            return false;
        }
        else if( pathB.startsWith( "../" ) )
        {
            return false;
        }
        else if( pathB.equals( pathA ) )
        {
            return true;
        }
        else if( pathA.isEmpty() )
        {
            return true;
        }
        else
        {
            return pathB.startsWith( pathA + "/" );
        }
    }

    public static String toLocal( String path, String location )
    {
        path = sanitizePath( path );
        location = sanitizePath( location );

        assert contains( location, path );
        String local = path.substring( location.length() );
        if( local.startsWith( "/" ) )
        {
            return local.substring( 1 );
        }
        else
        {
            return local;
        }
    }
}
