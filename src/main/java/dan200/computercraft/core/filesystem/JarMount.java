/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteStreams;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.shared.util.IoUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarMount implements IMount
{
    /**
     * Only cache files smaller than 1MiB.
     */
    private static final int MAX_CACHED_SIZE = 1 << 20;

    /**
     * Limit the entire cache to 64MiB.
     */
    private static final int MAX_CACHE_SIZE = 64 << 20;

    /**
     * We maintain a cache of the contents of all files in the mount. This allows us to allow
     * seeking within ROM files, and reduces the amount we need to access disk for computer startup.
     */
    private static final Cache<FileEntry, byte[]> CONTENTS_CACHE = CacheBuilder.newBuilder()
        .concurrencyLevel( 4 )
        .expireAfterAccess( 60, TimeUnit.SECONDS )
        .maximumWeight( MAX_CACHE_SIZE )
        .weakKeys()
        .<FileEntry, byte[]>weigher( ( k, v ) -> v.length )
        .build();

    /**
     * We have a {@link ReferenceQueue} of all mounts, a long with their corresponding {@link ZipFile}. If
     * the mount has been destroyed, we clean up after it.
     */
    private static final ReferenceQueue<JarMount> MOUNT_QUEUE = new ReferenceQueue<>();

    private final ZipFile zip;
    private final FileEntry root;

    public JarMount( File jarFile, String subPath ) throws IOException
    {
        // Cleanup any old mounts. It's unlikely that there will be any, but it's best to be safe.
        cleanup();

        if( !jarFile.exists() || jarFile.isDirectory() ) throw new FileNotFoundException( "Cannot find " + jarFile );

        // Open the zip file
        try
        {
            zip = new ZipFile( jarFile );
        }
        catch( IOException e )
        {
            throw new IOException( "Error loading zip file", e );
        }

        // Ensure the root entry exists.
        if( zip.getEntry( subPath ) == null )
        {
            zip.close();
            throw new FileNotFoundException( "Zip does not contain path" );
        }

        // We now create a weak reference to this mount. This is automatically added to the appropriate queue.
        new MountReference( this );

        // Read in all the entries
        root = new FileEntry();
        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
        while( zipEntries.hasMoreElements() )
        {
            ZipEntry entry = zipEntries.nextElement();

            String entryPath = entry.getName();
            if( !entryPath.startsWith( subPath ) ) continue;

            String localPath = FileSystem.toLocal( entryPath, subPath );
            create( entry, localPath );
        }
    }

    private FileEntry get( String path )
    {
        FileEntry lastEntry = root;
        int lastIndex = 0;

        while( lastEntry != null && lastIndex < path.length() )
        {
            int nextIndex = path.indexOf( '/', lastIndex );
            if( nextIndex < 0 ) nextIndex = path.length();

            lastEntry = lastEntry.children == null ? null : lastEntry.children.get( path.substring( lastIndex, nextIndex ) );
            lastIndex = nextIndex + 1;
        }

        return lastEntry;
    }

    private void create( ZipEntry entry, String localPath )
    {
        FileEntry lastEntry = root;

        int lastIndex = 0;
        while( lastIndex < localPath.length() )
        {
            int nextIndex = localPath.indexOf( '/', lastIndex );
            if( nextIndex < 0 ) nextIndex = localPath.length();

            String part = localPath.substring( lastIndex, nextIndex );
            if( lastEntry.children == null ) lastEntry.children = new HashMap<>( 0 );

            FileEntry nextEntry = lastEntry.children.get( part );
            if( nextEntry == null || !nextEntry.isDirectory() )
            {
                lastEntry.children.put( part, nextEntry = new FileEntry() );
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }

        lastEntry.setup( entry );
    }

    @Override
    public boolean exists( @Nonnull String path )
    {
        return get( path ) != null;
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        FileEntry file = get( path );
        return file != null && file.isDirectory();
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        FileEntry file = get( path );
        if( file == null || !file.isDirectory() ) throw new FileOperationException( path, "Not a directory" );

        file.list( contents );
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        FileEntry file = get( path );
        if( file != null ) return file.size;
        throw new FileOperationException( path, "No such file" );
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead( @Nonnull String path ) throws IOException
    {
        FileEntry file = get( path );
        if( file != null && !file.isDirectory() )
        {
            byte[] contents = CONTENTS_CACHE.getIfPresent( file );
            if( contents != null ) return new ArrayByteChannel( contents );

            try
            {
                ZipEntry entry = zip.getEntry( file.path );
                if( entry != null )
                {
                    try( InputStream stream = zip.getInputStream( entry ) )
                    {
                        if( stream.available() > MAX_CACHED_SIZE ) return Channels.newChannel( stream );

                        contents = ByteStreams.toByteArray( stream );
                        CONTENTS_CACHE.put( file, contents );
                        return new ArrayByteChannel( contents );
                    }
                }
            }
            catch( IOException e )
            {
                // Treat errors as non-existence of file
            }
        }

        throw new FileOperationException( path, "No such file" );
    }

    @Nonnull
    @Override
    public BasicFileAttributes getAttributes( @Nonnull String path ) throws IOException
    {
        FileEntry file = get( path );
        if( file != null )
        {
            ZipEntry entry = zip.getEntry( file.path );
            if( entry != null ) return new ZipEntryAttributes( entry );
        }

        throw new FileOperationException( path, "No such file" );
    }

    private static class FileEntry
    {
        String path;
        long size;
        Map<String, FileEntry> children;

        void setup( ZipEntry entry )
        {
            path = entry.getName();
            size = entry.getSize();
            if( children == null && entry.isDirectory() ) children = new HashMap<>( 0 );
        }

        boolean isDirectory()
        {
            return children != null;
        }

        void list( List<String> contents )
        {
            if( children != null ) contents.addAll( children.keySet() );
        }
    }

    private static class MountReference extends WeakReference<JarMount>
    {
        final ZipFile file;

        MountReference( JarMount file )
        {
            super( file, MOUNT_QUEUE );
            this.file = file.zip;
        }
    }

    private static void cleanup()
    {
        Reference<? extends JarMount> next;
        while( (next = MOUNT_QUEUE.poll()) != null ) IoUtil.closeQuietly( ((MountReference) next).file );
    }

    private static class ZipEntryAttributes implements BasicFileAttributes
    {
        private final ZipEntry entry;

        ZipEntryAttributes( ZipEntry entry )
        {
            this.entry = entry;
        }

        @Override
        public FileTime lastModifiedTime()
        {
            return orEpoch( entry.getLastModifiedTime() );
        }

        @Override
        public FileTime lastAccessTime()
        {
            return orEpoch( entry.getLastAccessTime() );
        }

        @Override
        public FileTime creationTime()
        {
            FileTime time = entry.getCreationTime();
            return time == null ? lastModifiedTime() : time;
        }

        @Override
        public boolean isRegularFile()
        {
            return !entry.isDirectory();
        }

        @Override
        public boolean isDirectory()
        {
            return entry.isDirectory();
        }

        @Override
        public boolean isSymbolicLink()
        {
            return false;
        }

        @Override
        public boolean isOther()
        {
            return false;
        }

        @Override
        public long size()
        {
            return entry.getSize();
        }

        @Override
        public Object fileKey()
        {
            return null;
        }

        private static final FileTime EPOCH = FileTime.from( Instant.EPOCH );

        private static FileTime orEpoch( FileTime time )
        {
            return time == null ? EPOCH : time;
        }
    }
}
