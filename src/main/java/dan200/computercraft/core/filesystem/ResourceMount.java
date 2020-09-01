/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.profiler.Profiler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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

    private static final MapMaker CACHE_TEMPLATE = new MapMaker().weakValues().concurrencyLevel( 1 );

    /**
     * Maintain a cache of currently loaded resource mounts. This cache is invalidated when currentManager changes.
     */
    private static final Map<ReloadableResourceManager, Map<Identifier, ResourceMount>> MOUNT_CACHE = new WeakHashMap<>( 2 );

    private final String namespace;
    private final String subPath;
    private final ReloadableResourceManager manager;

    @Nullable
    private FileEntry root;

    public static ResourceMount get( String namespace, String subPath, ReloadableResourceManager manager )
    {
        Map<Identifier, ResourceMount> cache;

        synchronized( MOUNT_CACHE )
        {
            cache = MOUNT_CACHE.get( manager );
            if( cache == null ) MOUNT_CACHE.put( manager, cache = CACHE_TEMPLATE.makeMap() );
        }

        Identifier path = new Identifier( namespace, subPath );
        synchronized( cache )
        {
            ResourceMount mount = cache.get( path );
            if( mount == null ) cache.put( path, mount = new ResourceMount( namespace, subPath, manager ) );
            return mount;
        }
    }

    private ResourceMount( String namespace, String subPath, ReloadableResourceManager manager )
    {
        this.namespace = namespace;
        this.subPath = subPath;
        this.manager = manager;

        Listener.INSTANCE.add( manager, this );
        if( root == null ) load();
    }

    private void load()
    {
        boolean hasAny = false;
        FileEntry newRoot = new FileEntry( new Identifier( namespace, subPath ) );
        for( Identifier file : manager.findResources( subPath, s -> true ) )
        {
            if( !file.getNamespace().equals( namespace ) ) continue;

            String localPath = FileSystem.toLocal( file.getPath(), subPath );
            create( newRoot, localPath );
            hasAny = true;
        }

        root = hasAny ? newRoot : null;
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
                Identifier childPath;
                try
                {
                    childPath = new Identifier( namespace, subPath + "/" + path );
                }
                catch( InvalidIdentifierException e )
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

            try( InputStream stream = manager.getResource( file.identifier ).getInputStream() )
            {
                if( stream.available() > MAX_CACHED_SIZE ) return Channels.newChannel( stream );

                contents = ByteStreams.toByteArray( stream );
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
        final Identifier identifier;
        Map<String, FileEntry> children;
        long size = -1;

        FileEntry( Identifier identifier )
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
     * While people should really be keeping a permanent reference to this, some people construct it every
     * method call, so let's make this as small as possible.
     */
    static class Listener implements ResourceReloadListener
    {
        private static final Listener INSTANCE = new Listener();

        private final Set<ResourceMount> mounts = Collections.newSetFromMap( new WeakHashMap<>() );
        private final Set<ReloadableResourceManager> managers = Collections.newSetFromMap( new WeakHashMap<>() );

        @Override
        public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager resourceManager, Profiler profiler, Profiler profiler1,
                                              Executor executor, Executor executor1) {
            return CompletableFuture.runAsync(() -> {
                profiler.push("Mount reloading");
                try {
                    for (ResourceMount mount : this.mounts) {
                        mount.load();
                    }
                } finally {
                    profiler.pop();
                }
            }, executor);
        }

        synchronized void add(ReloadableResourceManager manager, ResourceMount mount) {
            if (this.managers.add(manager)) {
                manager.registerListener(this);
            }
            this.mounts.add(mount);
        }
    }
}
