/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IWritableMount;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

public class FileMount implements IWritableMount
{
    private static final int MINIMUM_FILE_SIZE = 500;
    private static final Set<OpenOption> READ_OPTIONS = Collections.singleton( StandardOpenOption.READ );
    private static final Set<OpenOption> WRITE_OPTIONS = Sets.newHashSet( StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
    private static final Set<OpenOption> APPEND_OPTIONS = Sets.newHashSet( StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND );

    private class WritableCountingChannel implements WritableByteChannel
    {

        private final WritableByteChannel inner;
        long ignoredBytesLeft;

        WritableCountingChannel( WritableByteChannel inner, long bytesToIgnore )
        {
            this.inner = inner;
            ignoredBytesLeft = bytesToIgnore;
        }

        @Override
        public int write( @Nonnull ByteBuffer b ) throws IOException
        {
            count( b.remaining() );
            return inner.write( b );
        }

        void count( long n ) throws IOException
        {
            ignoredBytesLeft -= n;
            if( ignoredBytesLeft < 0 )
            {
                long newBytes = -ignoredBytesLeft;
                ignoredBytesLeft = 0;

                long bytesLeft = capacity - usedSpace;
                if( newBytes > bytesLeft ) throw new IOException( "Out of space" );
                usedSpace += newBytes;
            }
        }

        @Override
        public boolean isOpen()
        {
            return inner.isOpen();
        }

        @Override
        public void close() throws IOException
        {
            inner.close();
        }
    }

    private class SeekableCountingChannel extends WritableCountingChannel implements SeekableByteChannel
    {
        private final SeekableByteChannel inner;

        SeekableCountingChannel( SeekableByteChannel inner, long bytesToIgnore )
        {
            super( inner, bytesToIgnore );
            this.inner = inner;
        }

        @Override
        public SeekableByteChannel position( long newPosition ) throws IOException
        {
            if( !isOpen() ) throw new ClosedChannelException();
            if( newPosition < 0 )
            {
                throw new IllegalArgumentException( "Cannot seek before the beginning of the stream" );
            }

            long delta = newPosition - inner.position();
            if( delta < 0 )
            {
                ignoredBytesLeft -= delta;
            }
            else
            {
                count( delta );
            }

            return inner.position( newPosition );
        }

        @Override
        public SeekableByteChannel truncate( long size ) throws IOException
        {
            throw new IOException( "Not yet implemented" );
        }

        @Override
        public int read( ByteBuffer dst ) throws ClosedChannelException
        {
            if( !inner.isOpen() ) throw new ClosedChannelException();
            throw new NonReadableChannelException();
        }

        @Override
        public long position() throws IOException
        {
            return inner.position();
        }

        @Override
        public long size() throws IOException
        {
            return inner.size();
        }
    }

    private final File rootPath;
    private final long capacity;
    private long usedSpace;

    public FileMount( File rootPath, long capacity )
    {
        this.rootPath = rootPath;
        this.capacity = capacity + MINIMUM_FILE_SIZE;
        usedSpace = created() ? measureUsedSpace( this.rootPath ) : MINIMUM_FILE_SIZE;
    }

    // IMount implementation

    @Override
    public boolean exists( @Nonnull String path )
    {
        if( !created() ) return path.isEmpty();

        File file = getRealPath( path );
        return file.exists();
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        if( !created() ) return path.isEmpty();

        File file = getRealPath( path );
        return file.exists() && file.isDirectory();
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        if( !created() )
        {
            if( !path.isEmpty() ) throw new FileOperationException( path, "Not a directory" );
            return;
        }

        File file = getRealPath( path );
        if( !file.exists() || !file.isDirectory() ) throw new FileOperationException( path, "Not a directory" );

        String[] paths = file.list();
        for( String subPath : paths )
        {
            if( new File( file, subPath ).exists() ) contents.add( subPath );
        }
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        if( !created() )
        {
            if( path.isEmpty() ) return 0;
        }
        else
        {
            File file = getRealPath( path );
            if( file.exists() ) return file.isDirectory() ? 0 : file.length();
        }

        throw new FileOperationException( path, "No such file" );
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead( @Nonnull String path ) throws IOException
    {
        if( created() )
        {
            File file = getRealPath( path );
            if( file.exists() && !file.isDirectory() ) return FileChannel.open( file.toPath(), READ_OPTIONS );
        }

        throw new FileOperationException( path, "No such file" );
    }

    @Nonnull
    @Override
    public BasicFileAttributes getAttributes( @Nonnull String path ) throws IOException
    {
        if( created() )
        {
            File file = getRealPath( path );
            if( file.exists() ) return Files.readAttributes( file.toPath(), BasicFileAttributes.class );
        }

        throw new FileOperationException( path, "No such file" );
    }

    // IWritableMount implementation

    @Override
    public void makeDirectory( @Nonnull String path ) throws IOException
    {
        create();
        File file = getRealPath( path );
        if( file.exists() )
        {
            if( !file.isDirectory() ) throw new FileOperationException( path, "File exists" );
            return;
        }

        int dirsToCreate = 1;
        File parent = file.getParentFile();
        while( !parent.exists() )
        {
            ++dirsToCreate;
            parent = parent.getParentFile();
        }

        if( getRemainingSpace() < dirsToCreate * MINIMUM_FILE_SIZE )
        {
            throw new FileOperationException( path, "Out of space" );
        }

        if( file.mkdirs() )
        {
            usedSpace += dirsToCreate * MINIMUM_FILE_SIZE;
        }
        else
        {
            throw new FileOperationException( path, "Access denied" );
        }
    }

    @Override
    public void delete( @Nonnull String path ) throws IOException
    {
        if( path.isEmpty() ) throw new FileOperationException( path, "Access denied" );

        if( created() )
        {
            File file = getRealPath( path );
            if( file.exists() ) deleteRecursively( file );
        }
    }

    private void deleteRecursively( File file ) throws IOException
    {
        // Empty directories first
        if( file.isDirectory() )
        {
            String[] children = file.list();
            for( String aChildren : children )
            {
                deleteRecursively( new File( file, aChildren ) );
            }
        }

        // Then delete
        long fileSize = file.isDirectory() ? 0 : file.length();
        boolean success = file.delete();
        if( success )
        {
            usedSpace -= Math.max( MINIMUM_FILE_SIZE, fileSize );
        }
        else
        {
            throw new IOException( "Access denied" );
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openForWrite( @Nonnull String path ) throws IOException
    {
        create();
        File file = getRealPath( path );
        if( file.exists() && file.isDirectory() ) throw new FileOperationException( path, "Cannot write to directory" );

        if( file.exists() )
        {
            usedSpace -= Math.max( file.length(), MINIMUM_FILE_SIZE );
        }
        else if( getRemainingSpace() < MINIMUM_FILE_SIZE )
        {
            throw new FileOperationException( path, "Out of space" );
        }
        usedSpace += MINIMUM_FILE_SIZE;

        return new SeekableCountingChannel( Files.newByteChannel( file.toPath(), WRITE_OPTIONS ), MINIMUM_FILE_SIZE );
    }

    @Nonnull
    @Override
    public WritableByteChannel openForAppend( @Nonnull String path ) throws IOException
    {
        if( !created() )
        {
            throw new FileOperationException( path, "No such file" );
        }

        File file = getRealPath( path );
        if( !file.exists() ) throw new FileOperationException( path, "No such file" );
        if( file.isDirectory() ) throw new FileOperationException( path, "Cannot write to directory" );

        // Allowing seeking when appending is not recommended, so we use a separate channel.
        return new WritableCountingChannel(
            Files.newByteChannel( file.toPath(), APPEND_OPTIONS ),
            Math.max( MINIMUM_FILE_SIZE - file.length(), 0 )
        );
    }

    @Override
    public long getRemainingSpace()
    {
        return Math.max( capacity - usedSpace, 0 );
    }

    @Nonnull
    @Override
    public OptionalLong getCapacity()
    {
        return OptionalLong.of( capacity - MINIMUM_FILE_SIZE );
    }

    private File getRealPath( String path )
    {
        return new File( rootPath, path );
    }

    private boolean created()
    {
        return rootPath.exists();
    }

    private void create() throws IOException
    {
        if( !rootPath.exists() )
        {
            boolean success = rootPath.mkdirs();
            if( !success )
            {
                throw new IOException( "Access denied" );
            }
        }
    }

    private static class Visitor extends SimpleFileVisitor<Path>
    {
        long size;

        @Override
        public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs )
        {
            size += MINIMUM_FILE_SIZE;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
        {
            size += Math.max( attrs.size(), MINIMUM_FILE_SIZE );
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed( Path file, IOException exc )
        {
            ComputerCraft.log.error( "Error computing file size for {}", file, exc );
            return FileVisitResult.CONTINUE;
        }
    }

    private static long measureUsedSpace( File file )
    {
        if( !file.exists() ) return 0;

        try
        {
            Visitor visitor = new Visitor();
            Files.walkFileTree( file.toPath(), visitor );
            return visitor.size;
        }
        catch( IOException e )
        {
            ComputerCraft.log.error( "Error computing file size for {}", file, e );
            return 0;
        }
    }
}
