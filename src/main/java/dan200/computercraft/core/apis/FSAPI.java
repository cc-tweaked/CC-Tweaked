/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.apis.handles.BinaryInputHandle;
import dan200.computercraft.core.apis.handles.BinaryOutputHandle;
import dan200.computercraft.core.apis.handles.EncodedInputHandle;
import dan200.computercraft.core.apis.handles.EncodedOutputHandle;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

public class FSAPI implements ILuaAPI
{
    private IAPIEnvironment m_env;
    private FileSystem m_fileSystem;
    
    public FSAPI( IAPIEnvironment _env )
    {
        m_env = _env;
        m_fileSystem = null;
    }
    
    @Override
    public String[] getNames()
    {
        return new String[] {
            "fs"
        };
    }

    @Override
    public void startup( )
    {
        m_fileSystem = m_env.getFileSystem();
    }

    @Override
    public void shutdown( )
    {
        m_fileSystem = null;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "list",
            "combine",
            "getName",
            "getSize",
            "exists",
            "isDir",
            "isReadOnly",
            "makeDir",
            "move",
            "copy",
            "delete",
            "open",
            "getDrive",
            "getFreeSpace",
            "find",
            "getDir",
        };
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // list
                String path = getString( args, 0 );
                m_env.addTrackingChange( TrackingField.FS_OPS );
                try {
                    String[] results = m_fileSystem.list( path );
                    Map<Object,Object> table = new HashMap<>();
                    for(int i=0; i<results.length; ++i ) {
                        table.put( i+1, results[i] );
                    }
                    return MethodResult.of( table );
                }
                catch( FileSystemException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 1:
            {
                // combine
                String pathA = getString( args, 0 );
                String pathB = getString( args, 1 );
                return MethodResult.of( m_fileSystem.combine( pathA, pathB ) );
            }
            case 2:
            {
                // getName
                String path = getString( args, 0 );
                return MethodResult.of( FileSystem.getName( path ) );
            }
            case 3:
            {
                // getSize
                String path = getString( args, 0 );
                try
                {
                    return MethodResult.of( m_fileSystem.getSize( path ) );
                }
                catch( FileSystemException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 4:
            {
                // exists
                String path = getString( args, 0 );
                try {
                    return MethodResult.of( m_fileSystem.exists( path ) );
                } catch( FileSystemException e ) {
                    return MethodResult.of( false );
                }
            }
            case 5:
            {
                // isDir
                String path = getString( args, 0 );
                try {
                    return MethodResult.of( m_fileSystem.isDir( path ) );
                } catch( FileSystemException e ) {
                    return MethodResult.of( false );
                }
            }
            case 6:
            {
                // isReadOnly
                String path = getString( args, 0 );
                try {
                    return MethodResult.of( m_fileSystem.isReadOnly( path ) );
                } catch( FileSystemException e ) {
                    return MethodResult.of( false );
                }
            }
            case 7:
            {
                // makeDir
                String path = getString( args, 0 );
                try {
                    m_env.addTrackingChange( TrackingField.FS_OPS );
                    m_fileSystem.makeDir( path );
                    return MethodResult.empty();
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 8:
            {
                // move
                String path = getString( args, 0 );
                String dest = getString( args, 1 );
                try {
                    m_env.addTrackingChange( TrackingField.FS_OPS );
                    m_fileSystem.move( path, dest );
                    return MethodResult.empty();
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 9:
            {
                // copy
                String path = getString( args, 0 );
                String dest = getString( args, 1 );
                try {
                    m_env.addTrackingChange( TrackingField.FS_OPS );
                    m_fileSystem.copy( path, dest );
                    return MethodResult.empty();
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 10:
            {
                // delete
                String path = getString( args, 0 );
                try {
                    m_env.addTrackingChange( TrackingField.FS_OPS );
                    m_fileSystem.delete( path );
                    return MethodResult.empty();
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 11:
            {
                // open
                String path = getString( args, 0 );
                String mode = getString( args, 1 );
                m_env.addTrackingChange( TrackingField.FS_OPS );
                try {
                    switch( mode )
                    {
                        case "r":
                        {
                            // Open the file for reading, then create a wrapper around the reader
                            InputStream reader = m_fileSystem.openForRead( path );
                            return MethodResult.of( new EncodedInputHandle( reader ) );
                        }
                        case "w":
                        {
                            // Open the file for writing, then create a wrapper around the writer
                            OutputStream writer = m_fileSystem.openForWrite( path, false );
                            return MethodResult.of( new EncodedOutputHandle( writer ) );
                        }
                        case "a":
                        {
                            // Open the file for appending, then create a wrapper around the writer
                            OutputStream writer = m_fileSystem.openForWrite( path, true );
                            return MethodResult.of( new EncodedOutputHandle( writer ) );
                        }
                        case "rb":
                        {
                            // Open the file for binary reading, then create a wrapper around the reader
                            InputStream reader = m_fileSystem.openForRead( path );
                            return MethodResult.of( new BinaryInputHandle( reader ) );
                        }
                        case "wb":
                        {
                            // Open the file for binary writing, then create a wrapper around the writer
                            OutputStream writer = m_fileSystem.openForWrite( path, false );
                            return MethodResult.of( new BinaryOutputHandle( writer ) );
                        }
                        case "ab":
                        {
                            // Open the file for binary appending, then create a wrapper around the reader
                            OutputStream writer = m_fileSystem.openForWrite( path, true );
                            return MethodResult.of( new BinaryOutputHandle( writer ) );
                        }
                        default:
                            throw new LuaException( "Unsupported mode" );
                    }
                } catch( FileSystemException e ) {
                    return MethodResult.of( null, e.getMessage() );
                }
            }
            case 12:
            {
                // getDrive
                String path = getString( args, 0 );
                try {
                    if( !m_fileSystem.exists( path ) )
                    {
                        return MethodResult.empty();
                    }
                    return MethodResult.of( m_fileSystem.getMountLabel( path ) );
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 13:
            {
                // getFreeSpace
                String path = getString( args, 0 );
                try {
                    long freeSpace = m_fileSystem.getFreeSpace( path );
                    if( freeSpace >= 0 )
                    {
                        return MethodResult.of( freeSpace );
                    }
                    return MethodResult.of( "unlimited" );
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 14:
            {
                // find
                String path = getString( args, 0 );
                try {
                    m_env.addTrackingChange( TrackingField.FS_OPS );
                    String[] results = m_fileSystem.find( path );
                    Map<Object,Object> table = new HashMap<>();
                    for(int i=0; i<results.length; ++i ) {
                        table.put( i+1, results[i] );
                    }
                    return MethodResult.of( table );
                } catch( FileSystemException e ) {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 15:
            {
                // getDir
                String path = getString( args, 0 );
                return MethodResult.of( FileSystem.getDirectory( path ) );
            }
            default:
            {
                assert( false );
                return MethodResult.empty();
            }
        }
    }

    @Nullable
    @Override
    @Deprecated
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
    }
}
