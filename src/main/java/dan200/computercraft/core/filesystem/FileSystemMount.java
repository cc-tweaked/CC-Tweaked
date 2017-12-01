package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IFileSystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class FileSystemMount implements IFileSystem
{
    private final FileSystem m_filesystem;

    public FileSystemMount( FileSystem m_filesystem )
    {
        this.m_filesystem = m_filesystem;
    }

    @Override
    public void makeDirectory( @Nonnull String path ) throws IOException
    {
        try
        {
            m_filesystem.makeDir( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void delete( @Nonnull String path ) throws IOException
    {
        try
        {
            m_filesystem.delete( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public OutputStream openForWrite( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.openForWrite( path, false );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public OutputStream openForAppend( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.openForWrite( path, true );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public long getRemainingSpace() throws IOException
    {
        try
        {
            return m_filesystem.getFreeSpace( "/" );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public boolean exists( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.exists( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public boolean isDirectory( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.exists( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        try
        {
            Collections.addAll( contents, m_filesystem.list( path ) );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.getSize( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Nonnull
    @Override
    public InputStream openForRead( @Nonnull String path ) throws IOException
    {
        try
        {
            return m_filesystem.openForRead( path );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public String combine( String path, String child )
    {
        return m_filesystem.combine( path, child );
    }

    @Override
    public void copy( String from, String to ) throws IOException
    {
        try
        {
            m_filesystem.copy( from, to );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }

    @Override
    public void move( String from, String to ) throws IOException
    {
        try
        {
            m_filesystem.move( from, to );
        }
        catch( FileSystemException e )
        {
            throw new IOException( e.getMessage() );
        }
    }
}
