/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteStreams;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.shared.util.IoUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ResourceMount implements IMount
{
    /**
     * Only cache files smaller than 1MiB.
     */
    private static final int MAX_CACHED_SIZE = 1 << 20;

    /**
     * Limit the entire cache to 64MiB.
     */
    private static final int MAX_CACHE_SIZE = 64 << 20;

    private static final byte[] TEMP_BUFFER = new byte[8192];

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
     * Maintain a cache of currently loaded resource mounts. This cache is invalidated when currentManager changes.
     */
    private static final Map<ResourceLocation, ResourceMount> MOUNT_CACHE = new HashMap<>( 2 );

    private final String namespace;
    private final String subPath;
    private ResourceManager manager;

    @Nullable
    private FileEntry root;

    public static ResourceMount get( String namespace, String subPath, ResourceManager manager )
    {
        ResourceLocation path = new ResourceLocation( namespace, subPath );
        synchronized( MOUNT_CACHE )
        {
            ResourceMount mount = MOUNT_CACHE.get( path );
            if( mount == null ) MOUNT_CACHE.put( path, mount = new ResourceMount( namespace, subPath, manager ) );
            return mount;
        }
    }

    private ResourceMount( String namespace, String subPath, ResourceManager manager )
    {
        this.namespace = namespace;
        this.subPath = subPath;
        load( manager );
    }

    private void load( ResourceManager manager )
    {
        boolean hasAny = false;
        String existingNamespace = null;

        FileEntry newRoot = new FileEntry( new ResourceLocation( namespace, subPath ) );
        for( ResourceLocation file : manager.listResources( subPath, s -> true ) )
        {
            existingNamespace = file.getNamespace();

            if( !file.getNamespace().equals( namespace ) ) continue;
            if( !FileSystem.contains( subPath, file.getPath() ) ) continue; // Some packs seem to include the parent?

            String localPath = FileSystem.toLocal( file.getPath(), subPath );
            create( newRoot, localPath );
            hasAny = true;
        }

        this.manager = manager;
        root = hasAny ? newRoot : null;

        if( !hasAny )
        {
            ComputerCraft.log.warn( "Cannot find any files under /data/{}/{} for resource mount.", namespace, subPath );
            if( existingNamespace != null )
            {
                ComputerCraft.log.warn( "There are files under /data/{}/{} though. Did you get the wrong namespace?", existingNamespace, subPath );
            }
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

    private void create( FileEntry lastEntry, String path )
    {
        int lastIndex = 0;
        while( lastIndex < path.length() )
        {
            int nextIndex = path.indexOf( '/', lastIndex );
            if( nextIndex < 0 ) nextIndex = path.length();

            String part = path.substring( lastIndex, nextIndex );
            if( lastEntry.children == null ) lastEntry.children = new HashMap<>();

            FileEntry nextEntry = lastEntry.children.get( part );
            if( nextEntry == null )
            {
                ResourceLocation childPath;
                try
                {
                    childPath = new ResourceLocation( namespace, subPath + "/" + path );
                }
                catch( ResourceLocationException e )
                {
                    ComputerCraft.log.warn( "Cannot create resource location for {} ({})", part, e.getMessage() );
                    return;
                }
                lastEntry.children.put( part, nextEntry = new FileEntry( childPath ) );
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }
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
        if( file == null || !file.isDirectory() ) throw new IOException( "/" + path + ": Not a directory" );

        file.list( contents );
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        FileEntry file = get( path );
        if( file != null )
        {
            if( file.size != -1 ) return file.size;
            if( file.isDirectory() ) return file.size = 0;

            byte[] contents = CONTENTS_CACHE.getIfPresent( file );
            if( contents != null ) return file.size = contents.length;

            try
            {
                Resource resource = manager.getResource( file.identifier );
                InputStream s = resource.getInputStream();
                int total = 0, read = 0;
                do
                {
                    total += read;
                    read = s.read( TEMP_BUFFER );
                } while( read > 0 );

                return file.size = total;
            }
            catch( IOException e )
            {
                return file.size = 0;
            }
        }

        throw new IOException( "/" + path + ": No such file" );
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
                InputStream stream = manager.getResource( file.identifier ).getInputStream();
                if( stream.available() > MAX_CACHED_SIZE ) return Channels.newChannel( stream );

                try
                {
                    contents = ByteStreams.toByteArray( stream );
                }
                finally
                {
                    IoUtil.closeQuietly( stream );
                }

                CONTENTS_CACHE.put( file, contents );
                return new ArrayByteChannel( contents );
            }
            catch( FileNotFoundException ignored )
            {
            }
        }

        throw new IOException( "/" + path + ": No such file" );
    }

    private static class FileEntry
    {
        final ResourceLocation identifier;
        Map<String, FileEntry> children;
        long size = -1;

        FileEntry( ResourceLocation identifier )
        {
            this.identifier = identifier;
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

    /**
     * A {@link PreparableReloadListener} which reloads any associated mounts and correctly updates the resource manager
     * they point to.
     */
    public static final SimplePreparableReloadListener<Void> RELOAD_LISTENER = new SimplePreparableReloadListener<>()
    {
        @Nonnull
        @Override
        protected Void prepare( @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler )
        {
            profiler.push( "Reloading ComputerCraft mounts" );
            try
            {
                for( ResourceMount mount : MOUNT_CACHE.values() ) mount.load( manager );
            }
            finally
            {
                profiler.pop();
            }
            return null;
        }

        @Override
        protected void apply( @Nonnull Void result, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler )
        {
        }
    };
}
