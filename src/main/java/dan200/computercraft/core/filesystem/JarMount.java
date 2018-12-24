/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Deprecated
public class JarMount implements IMount
{
    private static class FileInZip
    {
        private String m_path;
        private boolean m_directory;
        private long m_size;
        private Map<String, FileInZip> m_children;

        public FileInZip( String path, boolean directory, long size )
        {
            m_path = path;
            m_directory = directory;
            m_size = m_directory ? 0 : size;
            m_children = new LinkedHashMap<>();
        }

        public String getPath()
        {
            return m_path;
        }

        public boolean isDirectory()
        {
            return m_directory;
        }

        public long getSize()
        {
            return m_size;
        }

        public void list( List<String> contents )
        {
            contents.addAll( m_children.keySet() );
        }

        public void insertChild( FileInZip child )
        {
            String localPath = FileSystem.toLocal( child.getPath(), m_path );
            m_children.put( localPath, child );
        }

        public FileInZip getFile( String path )
        {
            // If we've reached the target, return this
            if( path.equals( m_path ) )
            {
                return this;
            }

            // Otherwise, get the next component of the path
            String localPath = FileSystem.toLocal( path, m_path );
            int slash = localPath.indexOf( "/" );
            if( slash >= 0 )
            {
                localPath = localPath.substring( 0, slash );
            }

            // And recurse down using it
            FileInZip subFile = m_children.get( localPath );
            if( subFile != null )
            {
                return subFile.getFile( path );
            }

            return null;
        }

        public FileInZip getParent( String path )
        {
            if( path.length() == 0 )
            {
                return null;
            }

            FileInZip file = getFile( FileSystem.getDirectory( path ) );
            if( file.isDirectory() )
            {
                return file;
            }
            return null;
        }
    }

    private ZipFile m_zipFile;
    private FileInZip m_root;
    private String m_rootPath;

    @Deprecated
    public JarMount( File jarFile, String subPath ) throws IOException
    {
        if( !jarFile.exists() || jarFile.isDirectory() )
        {
            throw new FileNotFoundException();
        }

        // Open the zip file
        try
        {
            m_zipFile = new ZipFile( jarFile );
        }
        catch( Exception e )
        {
            throw new IOException( "Error loading zip file" );
        }

        if( m_zipFile.getEntry( subPath ) == null )
        {
            m_zipFile.close();
            throw new IOException( "Zip does not contain path" );
        }

        // Read in all the entries
        Enumeration<? extends ZipEntry> zipEntries = m_zipFile.entries();
        while( zipEntries.hasMoreElements() )
        {
            ZipEntry entry = zipEntries.nextElement();
            String entryName = entry.getName();
            if( entryName.startsWith( subPath ) )
            {
                entryName = FileSystem.toLocal( entryName, subPath );
                if( m_root == null )
                {
                    if( entryName.equals( "" ) )
                    {
                        m_root = new FileInZip( entryName, entry.isDirectory(), entry.getSize() );
                        m_rootPath = subPath;
                        if( !m_root.isDirectory() ) break;
                    }
                    else
                    {
                        // TODO: handle this case. The code currently assumes we find the root before anything else
                    }
                }
                else
                {
                    FileInZip parent = m_root.getParent( entryName );
                    if( parent != null )
                    {
                        parent.insertChild( new FileInZip( entryName, entry.isDirectory(), entry.getSize() ) );
                    }
                    else
                    {
                        // TODO: handle this case. The code currently assumes we find folders before their contents
                    }
                }
            }
        }
    }

    // IMount implementation

    @Override
    public boolean exists( @Nonnull String path )
    {
        FileInZip file = m_root.getFile( path );
        return file != null;
    }

    @Override
    public boolean isDirectory( @Nonnull String path )
    {
        FileInZip file = m_root.getFile( path );
        if( file != null )
        {
            return file.isDirectory();
        }
        return false;
    }

    @Override
    public void list( @Nonnull String path, @Nonnull List<String> contents ) throws IOException
    {
        FileInZip file = m_root.getFile( path );
        if( file != null && file.isDirectory() )
        {
            file.list( contents );
        }
        else
        {
            throw new IOException( "/" + path + ": Not a directory" );
        }
    }

    @Override
    public long getSize( @Nonnull String path ) throws IOException
    {
        FileInZip file = m_root.getFile( path );
        if( file != null )
        {
            return file.getSize();
        }
        throw new IOException( "/" + path + ": No such file" );
    }

    @Nonnull
    @Override
    public InputStream openForRead( @Nonnull String path ) throws IOException
    {
        FileInZip file = m_root.getFile( path );
        if( file != null && !file.isDirectory() )
        {
            try
            {
                String fullPath = m_rootPath;
                if( path.length() > 0 )
                {
                    fullPath = fullPath + "/" + path;
                }
                ZipEntry entry = m_zipFile.getEntry( fullPath );
                if( entry != null )
                {
                    return m_zipFile.getInputStream( entry );
                }
            }
            catch( Exception e )
            {
                // treat errors as non-existance of file
            }
        }
        throw new IOException( "/" + path + ": No such file" );
    }
}
