/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class ComputerAccess implements IComputerAccess
{
    private final IAPIEnvironment m_environment;
    private final Set<String> m_mounts = new HashSet<>();

    protected ComputerAccess( IAPIEnvironment environment )
    {
        this.m_environment = environment;
    }

    public void unmountAll()
    {
        FileSystem fileSystem = m_environment.getFileSystem();
        for( String mount : m_mounts )
        {
            fileSystem.unmount( mount );
        }
        m_mounts.clear();
    }

    @Override
    public synchronized String mount( @Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName )
    {
        Objects.requireNonNull( desiredLoc, "desiredLocation cannot be null" );
        Objects.requireNonNull( mount, "mount cannot be null" );
        Objects.requireNonNull( driveName, "driveName cannot be null" );

        // Mount the location
        String location;
        FileSystem fileSystem = m_environment.getFileSystem();
        if( fileSystem == null ) throw new IllegalStateException( "File system has not been created" );

        synchronized( fileSystem )
        {
            location = findFreeLocation( desiredLoc );
            if( location != null )
            {
                try
                {
                    fileSystem.mount( driveName, location, mount );
                }
                catch( FileSystemException ignored )
                {
                }
            }
        }

        if( location != null ) m_mounts.add( location );
        return location;
    }

    @Override
    public synchronized String mountWritable( @Nonnull String desiredLoc, @Nonnull IWritableMount mount, @Nonnull String driveName )
    {
        Objects.requireNonNull( desiredLoc, "desiredLocation cannot be null" );
        Objects.requireNonNull( mount, "mount cannot be null" );
        Objects.requireNonNull( driveName, "driveName cannot be null" );

        // Mount the location
        String location;
        FileSystem fileSystem = m_environment.getFileSystem();
        if( fileSystem == null ) throw new IllegalStateException( "File system has not been created" );

        synchronized( fileSystem )
        {
            location = findFreeLocation( desiredLoc );
            if( location != null )
            {
                try
                {
                    fileSystem.mountWritable( driveName, location, mount );
                }
                catch( FileSystemException ignored )
                {
                }
            }
        }

        if( location != null ) m_mounts.add( location );
        return location;
    }

    @Override
    public void unmount( String location )
    {
        if( location == null ) return;
        if( !m_mounts.contains( location ) ) throw new IllegalStateException( "You didn't mount this location" );

        m_environment.getFileSystem().unmount( location );
        m_mounts.remove( location );
    }

    @Override
    public int getID()
    {
        return m_environment.getComputerID();
    }

    @Override
    public void queueEvent( @Nonnull final String event, final Object[] arguments )
    {
        Objects.requireNonNull( event, "event cannot be null" );
        m_environment.queueEvent( event, arguments );
    }

    @Nullable
    @Override
    public IWorkMonitor getMainThreadMonitor()
    {
        return m_environment.getMainThreadMonitor();
    }

    private String findFreeLocation( String desiredLoc )
    {
        try
        {
            FileSystem fileSystem = m_environment.getFileSystem();
            if( !fileSystem.exists( desiredLoc ) ) return desiredLoc;

            // We used to check foo2, foo3, foo4, etc here but the disk drive does this itself now
            return null;
        }
        catch( FileSystemException e )
        {
            return null;
        }
    }
}
