// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import static dan200.computercraft.api.filesystem.MountConstants.*;

class MountWrapper {
    private final String label;
    private final String location;

    private final Mount mount;
    private final @Nullable WritableMount writableMount;

    MountWrapper(String label, String location, Mount mount) {
        this.label = label;
        this.location = location;
        this.mount = mount;
        writableMount = null;
    }

    MountWrapper(String label, String location, WritableMount mount) {
        this.label = label;
        this.location = location;
        this.mount = mount;
        writableMount = mount;
    }

    public String getLabel() {
        return label;
    }

    public String getLocation() {
        return location;
    }

    public long getFreeSpace() {
        if (writableMount == null) return 0;

        try {
            return writableMount.getRemainingSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    public OptionalLong getCapacity() {
        return writableMount == null ? OptionalLong.empty() : OptionalLong.of(writableMount.getCapacity());
    }

    public boolean isReadOnly(String path) throws FileSystemException {
        try {
            return writableMount == null || writableMount.isReadOnly(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public boolean exists(String path) throws FileSystemException {
        path = toLocal(path);
        try {
            return mount.exists(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public boolean isDirectory(String path) throws FileSystemException {
        path = toLocal(path);
        try {
            return mount.isDirectory(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void list(String path, List<String> contents) throws FileSystemException {
        path = toLocal(path);
        try {
            if (!mount.exists(path)) throw localExceptionOf(path, NO_SUCH_FILE);
            if (!mount.isDirectory(path)) throw localExceptionOf(path, NOT_A_DIRECTORY);

            mount.list(path, contents);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public long getSize(String path) throws FileSystemException {
        path = toLocal(path);
        try {
            return mount.getSize(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public BasicFileAttributes getAttributes(String path) throws FileSystemException {
        path = toLocal(path);
        try {
            return mount.getAttributes(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public SeekableByteChannel openForRead(String path) throws FileSystemException {
        path = toLocal(path);
        try {
            return mount.openForRead(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void makeDirectory(String path) throws FileSystemException {
        if (writableMount == null) throw new FileSystemException(path, ACCESS_DENIED);

        path = toLocal(path);
        try {
            writableMount.makeDirectory(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void delete(String path) throws FileSystemException {
        if (writableMount == null) throw new FileSystemException(path, ACCESS_DENIED);

        path = toLocal(path);
        try {
            writableMount.delete(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void rename(String source, String dest) throws FileSystemException {
        if (writableMount == null) throw new FileSystemException(source, ACCESS_DENIED);

        source = toLocal(source);
        dest = toLocal(dest);
        try {
            if (!dest.isEmpty()) {
                var destParent = FileSystem.getDirectory(dest);
                if (!destParent.isEmpty() && !mount.exists(destParent)) writableMount.makeDirectory(destParent);
            }

            writableMount.rename(source, dest);
        } catch (IOException e) {
            throw localExceptionOf(source, e);
        }
    }

    public SeekableByteChannel openForWrite(String path, Set<OpenOption> options) throws FileSystemException {
        if (writableMount == null) throw new FileSystemException(path, ACCESS_DENIED);

        path = toLocal(path);
        try {
            if (mount.isDirectory(path)) {
                throw localExceptionOf(path, options.contains(StandardOpenOption.CREATE) ? CANNOT_WRITE_TO_DIRECTORY : NOT_A_FILE);
            }
            if (options.contains(StandardOpenOption.CREATE)) {
                var dir = FileSystem.getDirectory(path);
                if (!dir.isEmpty() && !mount.exists(path)) writableMount.makeDirectory(dir);
            }

            return writableMount.openFile(path, options);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    private String toLocal(String path) {
        return FileSystem.toLocal(path, location);
    }

    private FileSystemException localExceptionOf(String localPath, IOException e) {
        if (e instanceof FileOperationException ex) {
            if (ex.getFilename() != null) return localExceptionOf(ex.getFilename(), FileSystemException.getMessage(ex));
        }

        if (e instanceof java.nio.file.FileSystemException ex) {
            // This error will contain the absolute path, leaking information about where MC is installed. We drop that,
            // just taking the reason. We assume that the error refers to the input path.
            return localExceptionOf(localPath, MountHelpers.getReason(ex));
        }

        return FileSystemException.of(e);
    }

    private FileSystemException localExceptionOf(String path, String message) {
        if (!location.isEmpty()) path = path.isEmpty() ? location : location + "/" + path;
        return new FileSystemException(path, message);
    }
}
