/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

public class FileSystemMount implements IMount
{
    private final Entry rootEntry;

    public FileSystemMount( FileSystem fileSystem, String root ) throws IOException
    {
        Path rootPath = fileSystem.getPath( root );
        rootEntry = new Entry( "", rootPath );
        populate( rootEntry );
    }

    private void populate( Entry root ) throws IOException
    {
        if( !root.directory ) return;

        Queue<Entry> entries = new ArrayDeque<>();
        entries.add( root );
        while( !entries.isEmpty() )
        {
            Entry entry = entries.remove();
            try( Stream<Path> childStream = Files.list( entry.path ) )
            {
                Iterator<Path> children = childStream.iterator();
                while( children.hasNext() )
                {
                    Path childPath = children.next();
                    Entry child = new Entry( childPath.getFileName().toString(), childPath );
                    entry.children.put( child.name, child );
                    if( child.directory ) entries.add( child );
                }
            }
        }
    }

    @Override
    public boolean exists( @Nonnull String path )
    {
        return getFile( path ) != null;
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        Entry entry = getFile( path );
        return entry != null && entry.directory;
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        Entry entry = getFile( path );
        if( entry == null || !entry.directory ) throw new IOException( "/" + path + ": Not a directory" );

        contents.addAll( entry.children.keySet() );
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        Entry file = getFile( path );
        if( file == null ) throw new IOException( "/" + path + ": No such file" );
        return file.size;
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead( @Nonnull String path ) throws IOException
    {
        Entry file = getFile( path );
        if( file == null || file.directory ) throw new IOException( "/" + path + ": No such file" );

        return Files.newInputStream( file.path, StandardOpenOption.READ );
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead( @Nonnull String path ) throws IOException
    {
        Entry file = getFile( path );
        if( file == null || file.directory ) throw new IOException( "/" + path + ": No such file" );

        return Files.newByteChannel( file.path, StandardOpenOption.READ );
    }

    private Entry getFile( String path )
    {
        if( path.equals( "" ) ) return rootEntry;
        if( !path.contains( "/" ) ) return rootEntry.children.get( path );

        String[] components = path.split( "/" );
        Entry entry = rootEntry;
        for( String component : components )
        {
            if( entry == null || entry.children == null ) return null;
            entry = entry.children.get( component );
        }

        return entry;
    }

    private static class Entry
    {
        final String name;
        final Path path;

        final boolean directory;
        final long size;
        final Map<String, Entry> children;

        private Entry( String name, Path path ) throws IOException
        {
            if( name.endsWith( "/" ) || name.endsWith( "\\" ) ) name = name.substring( 0, name.length() - 1 );

            this.name = name;
            this.path = path;

            BasicFileAttributes attributes = Files.readAttributes( path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS );
            this.directory = attributes.isDirectory();
            this.size = directory ? 0 : attributes.size();
            this.children = directory ? new HashMap<>() : null;
        }
    }
}
