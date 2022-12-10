/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.OptionalLong;

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
            if (!mount.exists(path) || !mount.isDirectory(path)) {
                throw localExceptionOf(path, "Not a directory");
            }

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
        if (writableMount == null) throw exceptionOf(path, "Access denied");

        path = toLocal(path);
        try {
            writableMount.makeDirectory(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void delete(String path) throws FileSystemException {
        if (writableMount == null) throw exceptionOf(path, "Access denied");

        path = toLocal(path);
        try {
            writableMount.delete(path);
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public void rename(String source, String dest) throws FileSystemException {
        if (writableMount == null) throw exceptionOf(source, "Access denied");

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

    public SeekableByteChannel openForWrite(String path) throws FileSystemException {
        if (writableMount == null) throw exceptionOf(path, "Access denied");

        path = toLocal(path);
        try {
            if (mount.exists(path) && mount.isDirectory(path)) {
                throw localExceptionOf(path, "Cannot write to directory");
            } else {
                if (!path.isEmpty()) {
                    var dir = FileSystem.getDirectory(path);
                    if (!dir.isEmpty() && !mount.exists(path)) {
                        writableMount.makeDirectory(dir);
                    }
                }
                return writableMount.openForWrite(path);
            }
        } catch (IOException e) {
            throw localExceptionOf(path, e);
        }
    }

    public SeekableByteChannel openForAppend(String path) throws FileSystemException {
        if (writableMount == null) throw exceptionOf(path, "Access denied");

        path = toLocal(path);
        try {
            if (!mount.exists(path)) {
                if (!path.isEmpty()) {
                    var dir = FileSystem.getDirectory(path);
                    if (!dir.isEmpty() && !mount.exists(path)) {
                        writableMount.makeDirectory(dir);
                    }
                }
                return writableMount.openForWrite(path);
            } else if (mount.isDirectory(path)) {
                throw localExceptionOf(path, "Cannot write to directory");
            } else {
                return writableMount.openForAppend(path);
            }
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
            var message = ex.getReason();
            if (message == null) message = "Access denied";
            return localExceptionOf(localPath, message);
        }

        return FileSystemException.of(e);
    }

    private FileSystemException localExceptionOf(String path, String message) {
        if (!location.isEmpty()) path = path.isEmpty() ? location : location + "/" + path;
        return exceptionOf(path, message);
    }

    private static FileSystemException exceptionOf(String path, String message) {
        return new FileSystemException("/" + path + ": " + message);
    }
}
