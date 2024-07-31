// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.filesystem.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class ComputerAccess implements IComputerAccess {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerAccess.class);

    private final IAPIEnvironment environment;
    private final Set<String> mounts = new HashSet<>(0);

    protected ComputerAccess(IAPIEnvironment environment) {
        this.environment = environment;
    }

    public void unmountAll() {
        var fileSystem = environment.getFileSystem();
        if (!mounts.isEmpty()) {
            LOG.warn("Peripheral or API called mount but did not call unmount for {}", mounts);
        }

        for (var mount : mounts) {
            fileSystem.unmount(mount);
        }
        mounts.clear();
    }

    @Nullable
    @Override
    public synchronized String mount(String desiredLoc, Mount mount, String driveName) {
        Objects.requireNonNull(desiredLoc, "desiredLocation cannot be null");
        Objects.requireNonNull(mount, "mount cannot be null");
        Objects.requireNonNull(driveName, "driveName cannot be null");

        // Mount the location
        String location;
        var fileSystem = environment.getFileSystem();

        synchronized (fileSystem) {
            location = findFreeLocation(desiredLoc);
            if (location != null) {
                try {
                    fileSystem.mount(driveName, location, mount);
                } catch (FileSystemException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

        if (location != null) mounts.add(location);
        return location;
    }

    @Nullable
    @Override
    public synchronized String mountWritable(String desiredLoc, WritableMount mount, String driveName) {
        Objects.requireNonNull(desiredLoc, "desiredLocation cannot be null");
        Objects.requireNonNull(mount, "mount cannot be null");
        Objects.requireNonNull(driveName, "driveName cannot be null");

        // Mount the location
        String location;
        var fileSystem = environment.getFileSystem();

        synchronized (fileSystem) {
            location = findFreeLocation(desiredLoc);
            if (location != null) {
                try {
                    fileSystem.mountWritable(driveName, location, mount);
                } catch (FileSystemException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

        if (location != null) mounts.add(location);
        return location;
    }

    @Override
    public void unmount(@Nullable String location) {
        if (location == null) return;
        if (!mounts.contains(location)) throw new IllegalStateException("You didn't mount this location");

        environment.getFileSystem().unmount(location);
        mounts.remove(location);
    }

    @Override
    public int getID() {
        return environment.getComputerID();
    }

    @Override
    public void queueEvent(String event, @Nullable Object... arguments) {
        Objects.requireNonNull(event, "event cannot be null");
        environment.queueEvent(event, arguments);
    }

    @Override
    public WorkMonitor getMainThreadMonitor() {
        return environment.getMainThreadMonitor();
    }

    @Nullable
    private String findFreeLocation(String desiredLoc) {
        try {
            var fileSystem = environment.getFileSystem();
            if (!fileSystem.exists(desiredLoc)) return desiredLoc;

            // We used to check foo2, foo3, foo4, etc here but the disk drive does this itself now
            return null;
        } catch (FileSystemException e) {
            return null;
        }
    }
}
