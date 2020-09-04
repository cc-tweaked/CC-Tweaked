/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;

public abstract class ComputerAccess implements IComputerAccess {
    private final IAPIEnvironment m_environment;
    private final Set<String> m_mounts = new HashSet<>();

    protected ComputerAccess(IAPIEnvironment environment) {
        this.m_environment = environment;
    }

    public void unmountAll() {
        FileSystem fileSystem = this.m_environment.getFileSystem();
        for (String mount : this.m_mounts) {
            fileSystem.unmount(mount);
        }
        this.m_mounts.clear();
    }

    @Override
    public synchronized String mount(@Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName) {
        Objects.requireNonNull(desiredLoc, "desiredLocation cannot be null");
        Objects.requireNonNull(mount, "mount cannot be null");
        Objects.requireNonNull(driveName, "driveName cannot be null");

        // Mount the location
        String location;
        FileSystem fileSystem = this.m_environment.getFileSystem();
        if (fileSystem == null) {
            throw new IllegalStateException("File system has not been created");
        }

        synchronized (fileSystem) {
            location = this.findFreeLocation(desiredLoc);
            if (location != null) {
                try {
                    fileSystem.mount(driveName, location, mount);
                } catch (FileSystemException ignored) {
                }
            }
        }

        if (location != null) {
            this.m_mounts.add(location);
        }
        return location;
    }

    @Override
    public synchronized String mountWritable(@Nonnull String desiredLoc, @Nonnull IWritableMount mount, @Nonnull String driveName) {
        Objects.requireNonNull(desiredLoc, "desiredLocation cannot be null");
        Objects.requireNonNull(mount, "mount cannot be null");
        Objects.requireNonNull(driveName, "driveName cannot be null");

        // Mount the location
        String location;
        FileSystem fileSystem = this.m_environment.getFileSystem();
        if (fileSystem == null) {
            throw new IllegalStateException("File system has not been created");
        }

        synchronized (fileSystem) {
            location = this.findFreeLocation(desiredLoc);
            if (location != null) {
                try {
                    fileSystem.mountWritable(driveName, location, mount);
                } catch (FileSystemException ignored) {
                }
            }
        }

        if (location != null) {
            this.m_mounts.add(location);
        }
        return location;
    }

    @Override
    public void unmount(String location) {
        if (location == null) {
            return;
        }
        if (!this.m_mounts.contains(location)) {
            throw new IllegalStateException("You didn't mount this location");
        }

        this.m_environment.getFileSystem()
                          .unmount(location);
        this.m_mounts.remove(location);
    }

    @Override
    public int getID() {
        return this.m_environment.getComputerID();
    }

    @Override
    public void queueEvent(@Nonnull String event, Object... arguments) {
        Objects.requireNonNull(event, "event cannot be null");
        this.m_environment.queueEvent(event, arguments);
    }

    @Nonnull
    @Override
    public IWorkMonitor getMainThreadMonitor() {
        return this.m_environment.getMainThreadMonitor();
    }

    private String findFreeLocation(String desiredLoc) {
        try {
            FileSystem fileSystem = this.m_environment.getFileSystem();
            if (!fileSystem.exists(desiredLoc)) {
                return desiredLoc;
            }

            // We used to check foo2, foo3, foo4, etc here but the disk drive does this itself now
            return null;
        } catch (FileSystemException e) {
            return null;
        }
    }
}
