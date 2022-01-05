/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class ComputerAccess implements IComputerAccess
{
    private final IAPIEnvironment environment;
    private final Set<String> mounts = new HashSet<>();

    protected ComputerAccess( IAPIEnvironment environment )
    {
        this.environment = environment;
    }

    public void unmountAll()
    {
        FileSystem fileSystem = environment.getFileSystem();
        if( !mounts.isEmpty() )
        {
            ComputerCraft.log.warn( "Peripheral or API called mount but did not call unmount for {}", mounts );
        }

        for( String mount : mounts )
        {
            fileSystem.unmount( mount );
        }
        mounts.clear();
    }

    @Override
    public synchronized String mount( @Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName )
    {
        Objects.requireNonNull( desiredLoc, "desiredLocation cannot be null" );
        Objects.requireNonNull( mount, "mount cannot be null" );
        Objects.requireNonNull( driveName, "driveName cannot be null" );

        // Mount the location
        String location;
        FileSystem fileSystem = environment.getFileSystem();
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

        if( location != null ) mounts.add( location );
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
        FileSystem fileSystem = environment.getFileSystem();
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

        if( location != null ) mounts.add( location );
        return location;
    }

    @Override
    public void unmount( String location )
    {
        if( location == null ) return;
        if( !mounts.contains( location ) ) throw new IllegalStateException( "You didn't mount this location" );

        environment.getFileSystem().unmount( location );
        mounts.remove( location );
    }

    @Override
    public int getID()
    {
        return environment.getComputerID();
    }

    @Override
    public void queueEvent( @Nonnull String event, Object... arguments )
    {
        Objects.requireNonNull( event, "event cannot be null" );
        environment.queueEvent( event, arguments );
    }

    @Nonnull
    @Override
    public IWorkMonitor getMainThreadMonitor()
    {
        return environment.getMainThreadMonitor();
    }

    private String findFreeLocation( String desiredLoc )
    {
        try
        {
            FileSystem fileSystem = environment.getFileSystem();
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
