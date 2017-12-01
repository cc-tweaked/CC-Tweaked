/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class FileSystem
{    
    private class MountWrapper
    {
        private String m_label;
        private String m_location;
        
        private IMount m_mount;
        private IWritableMount m_writableMount;
        
        public MountWrapper( String label, String location, IMount mount )
        {
            m_label = label;
            m_location = location;
            m_mount = mount;
            m_writableMount = null;
        }

        public MountWrapper( String label, String location, IWritableMount mount )
        {
            this( label, location, (IMount)mount );
            m_writableMount = mount;
        }
        
        public String getLabel()
        {
            return m_label;
        }

        public String getLocation()
        {
            return m_location;
        }
        
        public long getFreeSpace()
        {
            if( m_writableMount == null )
            {
                return 0;
            }
                        
            try
            {
                return m_writableMount.getRemainingSpace();
            }
            catch( IOException e )
            {
                return 0;
            }
        }
        
        public boolean isReadOnly( String path )
        {
            return (m_writableMount == null);
        }
                
        // IMount forwarders:
        
        public boolean exists( String path ) throws FileSystemException
        {
            path = toLocal( path );
            try
            {
                return m_mount.exists( path );
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
        
        public boolean isDirectory( String path ) throws FileSystemException
        {
            path = toLocal( path );
            try
            {
                return m_mount.exists( path ) && m_mount.isDirectory( path );
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
        
        public void list( String path, List<String> contents ) throws FileSystemException
        {
            path = toLocal( path );
            try
            {
                if( m_mount.exists( path ) && m_mount.isDirectory( path ) )
                {
                    m_mount.list( path, contents );
                }
                else
                {
                    throw new FileSystemException( "/" + path + ": Not a directory" );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
        
        public long getSize( String path ) throws FileSystemException
        {
            path = toLocal( path );
            try
            {
                if( m_mount.exists( path ) )
                {
                    if( m_mount.isDirectory( path ) )
                    {
                        return 0;
                    }
                    else
                    {
                        return m_mount.getSize( path );
                    }
                }
                else
                {
                    throw new FileSystemException( "/" + path + ": No such file" );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
    
        public InputStream openForRead( String path ) throws FileSystemException
        {
            path = toLocal( path );
            try
            {
                if( m_mount.exists( path ) && !m_mount.isDirectory( path ) )
                {
                    return m_mount.openForRead( path );
                }
                else
                {
                    throw new FileSystemException( "/" + path + ": No such file" );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
        
        // IWritableMount forwarders:
                
        public void makeDirectory( String path ) throws FileSystemException
        {
            if( m_writableMount == null )
            {
                throw new FileSystemException( "/" + path + ": Access denied" );
            }
            try
            {
                path = toLocal( path );
                if( m_mount.exists( path ) )
                {
                    if( !m_mount.isDirectory( path ) )
                    {
                        throw new FileSystemException( "/" + path + ": File exists" );
                    }
                }
                else
                {
                    m_writableMount.makeDirectory( path );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
                
        public void delete( String path ) throws FileSystemException
        {
            if( m_writableMount == null )
            {
                throw new FileSystemException( "/" + path + ": Access denied" );
            }
            try
            {
                path = toLocal( path );
                if( m_mount.exists( path ) )
                {
                    m_writableMount.delete( path );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
    
        public OutputStream openForWrite( String path ) throws FileSystemException
        {
            if( m_writableMount == null )
            {
                throw new FileSystemException( "/" + path + ": Access denied" );
            }
            try
            {
                path = toLocal( path );
                if( m_mount.exists( path ) && m_mount.isDirectory( path ) )
                {
                    throw new FileSystemException( "/" + path + ": Cannot write to directory" );
                }
                else
                {
                    if( !path.isEmpty() )
                    {
                        String dir = getDirectory( path );
                        if( !dir.isEmpty() && !m_mount.exists( path ) )
                        {
                            m_writableMount.makeDirectory( dir );
                        }
                    }
                    return m_writableMount.openForWrite( path );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
        }
        
        public OutputStream openForAppend( String path ) throws FileSystemException
        {
            if( m_writableMount == null )
            {
                throw new FileSystemException( "/" + path + ": Access denied" );
            }
            try
            {
                path = toLocal( path );
                if( !m_mount.exists( path ) )
                {
                    if( !path.isEmpty() )
                    {
                        String dir = getDirectory( path );
                        if( !dir.isEmpty() && !m_mount.exists( path ) )
                        {
                            m_writableMount.makeDirectory( dir );
                        }
                    }
                    return m_writableMount.openForWrite( path );
                }
                else if( m_mount.isDirectory( path ) )
                {
                    throw new FileSystemException( "/" + path + ": Cannot write to directory" );
                }
                else
                {
                    return m_writableMount.openForAppend( path );
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }        
        }
    
        // private members
        
        private String toLocal( String path )
        {
            return FileSystem.toLocal( path, m_location );
        }
    }

    private final FileSystemMount m_wrapper = new FileSystemMount( this );
    private final Map<String, MountWrapper> m_mounts = new HashMap<>();
    private final Set<Closeable> m_openFiles = Collections.newSetFromMap( new WeakHashMap<Closeable, Boolean>() );
    
    public FileSystem( String rootLabel, IMount rootMount ) throws FileSystemException
    {
        mount( rootLabel, "", rootMount );
    }
    
    public FileSystem( String rootLabel, IWritableMount rootMount ) throws FileSystemException
    {
        mountWritable( rootLabel, "", rootMount );
    }
    
    public void unload()
    {
        // Close all dangling open files
        synchronized( m_openFiles )
        {
            for( Closeable file : m_openFiles )
            {
                try {
                    file.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            m_openFiles.clear();
        }
    }
    
    public synchronized void mount( String label, String location, IMount mount ) throws FileSystemException
    {
        if( mount == null )
        {
            throw new NullPointerException();
        }
        location = sanitizePath( location );
        if( location.contains( ".." ) ) {
            throw new FileSystemException( "Cannot mount below the root" );
        }                    
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
        if( m_mounts.containsKey( location ) )
        {
            m_mounts.remove( location );
        }
        m_mounts.put( location, wrapper );
    }
        
    public synchronized void unmount( String path )
    {
        path = sanitizePath( path );
        if( m_mounts.containsKey( path ) )
        {
            m_mounts.remove( path );
        }
    }
        
    public synchronized String combine( String path, String childPath )
    {
        path = sanitizePath( path, true );
        childPath = sanitizePath( childPath, true );
        
        if( path.isEmpty() ) {
            return childPath;
        } else if( childPath.isEmpty() ) {
            return path;
        } else {
            return sanitizePath( path + '/' + childPath, true );
        }
    }
        
    public static String getDirectory( String path )
    {
        path = sanitizePath( path, true );
        if( path.isEmpty() ) {
            return "..";
        }
        
        int lastSlash = path.lastIndexOf('/');
        if( lastSlash >= 0 ) {
            return path.substring( 0, lastSlash );
        } else {
            return "";
        }
    }

    public static String getName( String path )
    {
        path = sanitizePath( path, true );
        if( path.isEmpty() ) {
            return "root";
        }
        
        int lastSlash = path.lastIndexOf('/');
        if( lastSlash >= 0 ) {
            return path.substring( lastSlash + 1 );
        } else {
            return path;
        }
    }
    
    public synchronized long getSize( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.getSize( path );
    }
    
    public synchronized String[] list( String path ) throws FileSystemException
    {    
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        
        // Gets a list of the files in the mount
        List<String> list = new ArrayList<>();
        mount.list( path, list );
        
        // Add any mounts that are mounted at this location
        for( MountWrapper otherMount : m_mounts.values() )
        {
            if( getDirectory( otherMount.getLocation() ).equals( path ) )
            {
                list.add( getName( otherMount.getLocation() ) );
            }
        }
        
        // Return list
        String[] array = new String[ list.size() ];
        list.toArray(array);
        Arrays.sort( array );
        return array;
    }

    private void findIn( String dir, List<String> matches, Pattern wildPattern ) throws FileSystemException
    {
        String[] list = list( dir );
        for( String entry : list )
        {
            String entryPath = dir.isEmpty() ? entry : (dir + "/" + entry);
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
            return exists( wildPath ) ? new String[]{wildPath} : new String[0];
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
        String[] array = new String[ matches.size() ];
        matches.toArray(array);
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
        if( isReadOnly( sourcePath ) || isReadOnly( destPath ) ) {
            throw new FileSystemException( "Access denied" );
        }
        if( !exists( sourcePath ) ) {
            throw new FileSystemException( "No such file" );
        }
        if( exists( destPath ) ) {
            throw new FileSystemException( "File exists" );
        }
        if( contains( sourcePath, destPath ) ) {
            throw new FileSystemException( "Can't move a directory inside itself" );
        }
        copy( sourcePath, destPath );
        delete( sourcePath );
    }
        
    public synchronized void copy( String sourcePath, String destPath ) throws FileSystemException
    {
        sourcePath = sanitizePath( sourcePath );
        destPath = sanitizePath( destPath );
        if( isReadOnly( destPath ) ) {
            throw new FileSystemException( "/" + destPath + ": Access denied" );
        }
        if( !exists( sourcePath ) ) {
            throw new FileSystemException( "/" + sourcePath + ": No such file" );
        }
        if( exists( destPath ) ) {
            throw new FileSystemException( "/" + destPath + ": File exists" );
        }
        if( contains( sourcePath, destPath ) ) {
            throw new FileSystemException( "/" + sourcePath + ": Can't copy a directory inside itself" );
        }
        copyRecursive( sourcePath, getMount( sourcePath ), destPath, getMount( destPath ) );
    }

    private synchronized void copyRecursive( String sourcePath, MountWrapper sourceMount, String destinationPath, MountWrapper destinationMount ) throws FileSystemException
    {
        if( !sourceMount.exists( sourcePath ) )
        {
            return;
        }
        
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
                    combine( destinationPath, child ), destinationMount
                );
            }
        }
        else
        {
            // Copy a file:
            InputStream source = null;
            OutputStream destination = null;
            try
            {
                // Open both files
                source = sourceMount.openForRead( sourcePath );
                destination = destinationMount.openForWrite( destinationPath );
            
                // Copy bytes as fast as we can
                byte[] buffer = new byte[1024];
                while( true )
                {
                    int bytesRead = source.read( buffer );
                    if( bytesRead >= 0 )
                    {
                        destination.write( buffer, 0, bytesRead );
                    }
                    else
                    {
                        break;
                    }
                }
            }
            catch( IOException e )
            {
                throw new FileSystemException( e.getMessage() );
            }
            finally
            {
                // Close both files
                if( source != null )
                {
                    try {
                        source.close();
                    } catch( IOException e ) {
                        // nobody cares
                    }
                }
                if( destination != null )
                {
                    try {
                        destination.close();
                    } catch( IOException e ) {
                        // nobody cares
                    }
                }
            }
        }
    }

    private synchronized <T> T openFile( T file, Closeable handle ) throws FileSystemException
    {
        synchronized( m_openFiles )
        {
            if( ComputerCraft.maximumFilesOpen > 0 &&
                m_openFiles.size() >= ComputerCraft.maximumFilesOpen )
            {
                if( handle != null )
                {
                    try {
                        handle.close();
                    } catch ( IOException ignored ) {
                        // shrug
                    }
                }
                throw new FileSystemException("Too many files already open");
            }

            m_openFiles.add( handle );
            return file;
        }
    }

    private synchronized void closeFile( Closeable handle ) throws IOException
    {
        synchronized( m_openFiles )
        {
            m_openFiles.remove( handle );
            handle.close();
        }
    }
    
    public synchronized InputStream openForRead( String path ) throws FileSystemException
    {
        path = sanitizePath ( path );
        MountWrapper mount = getMount( path );
        InputStream stream = mount.openForRead( path );
        if( stream != null )
        {
            return openFile( new ClosingInputStream( stream ), stream );
        }
        return null;
    }

    public synchronized OutputStream openForWrite( String path, boolean append ) throws FileSystemException
    {
        path = sanitizePath ( path );
        MountWrapper mount = getMount( path );
        OutputStream stream = append ? mount.openForAppend( path ) : mount.openForWrite( path );
        if( stream != null )
        {
            return openFile( new ClosingOutputStream( stream ), stream );
        }
        return null;
    }

    public long getFreeSpace( String path ) throws FileSystemException
    {
        path = sanitizePath( path );
        MountWrapper mount = getMount( path );
        return mount.getFreeSpace();
    }
        
    private MountWrapper getMount( String path ) throws FileSystemException
    {
        // Return the deepest mount that contains a given path
        Iterator<MountWrapper> it = m_mounts.values().iterator();
        MountWrapper match = null;
        int matchLength = 999;
        while( it.hasNext() )
        {
            MountWrapper mount = it.next();
            if( contains( mount.getLocation(), path ) ) {
                int len = toLocal( path, mount.getLocation() ).length();
                if( match == null || len < matchLength ) {
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
        return m_wrapper;
    }

    private static String sanitizePath( String path )
    {
        return sanitizePath( path, false );
    }

    private static final Pattern threeDotsPattern = Pattern.compile( "^\\.{3,}$" );
    private static String sanitizePath( String path, boolean allowWildcards )
    {
        // Allow windowsy slashes
        path = path.replace( '\\', '/' );
        
        // Clean the path or illegal characters.
        final char[] specialChars = {
            '"', ':', '<', '>', '?', '|' // Sorted by ascii value (important)
        };

        StringBuilder cleanName = new StringBuilder();
        for( int i = 0; i < path.length(); i++ ) {
            char c = path.charAt(i);
            if( c >= 32 && Arrays.binarySearch( specialChars, c ) < 0 && (allowWildcards || c != '*') )
            {
                cleanName.append( c );
            }
        }
        path = cleanName.toString();
        
        // Collapse the string into its component parts, removing ..'s
        String[] parts = path.split("/");
        Stack<String> outputParts = new Stack<>();
        for( String part : parts )
        {
            if( part.length() == 0 || part.equals( "." ) || threeDotsPattern.matcher( part ).matches() ) 
            {
                // . is redundant
                // ... and more are treated as .
                continue;
            }
            else if( part.equals( ".." ) )
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
        StringBuilder result = new StringBuilder( "" );
        Iterator<String> it = outputParts.iterator();
        while( it.hasNext() ) {
            String part = it.next();
            result.append( part );
            if( it.hasNext() ) {
                result.append( '/' );
            }
        }

        return result.toString();
    }
    
    public static boolean contains( String pathA, String pathB )
    {
        pathA = sanitizePath( pathA );
        pathB = sanitizePath( pathB );

        if( pathB.equals("..") )
        {
            return false;
        }
        else if ( pathB.startsWith("../") )
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
        
        assert( contains( location, path ) );    
        String local = path.substring( location.length() );
        if( local.startsWith("/") ) {
            return local.substring( 1 );
        } else {
            return local;
        }
    }

    private class ClosingInputStream extends FilterInputStream
    {
        protected ClosingInputStream( InputStream in )
        {
            super( in );
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            closeFile( in );
        }
    }

    private class ClosingOutputStream extends FilterOutputStream
    {
        protected ClosingOutputStream( OutputStream out )
        {
            super( out );
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            closeFile( out );
        }
    }
}
