/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

class MountWrapper {
    private final String label;
    private final String location;

    private final IMount mount;
    private final IWritableMount writableMount;

    MountWrapper(String label, String location, IMount mount) {
        this.label = label;
        this.location = location;
        this.mount = mount;
        this.writableMount = null;
    }

    MountWrapper(String label, String location, IWritableMount mount) {
        this.label = label;
        this.location = location;
        this.mount = mount;
        this.writableMount = mount;
    }

    public String getLabel() {
        return this.label;
    }

    public String getLocation() {
        return this.location;
    }

    public long getFreeSpace() {
        if (this.writableMount == null) {
            return 0;
        }

        try {
            return this.writableMount.getRemainingSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    public OptionalLong getCapacity() {
        return this.writableMount == null ? OptionalLong.empty() : this.writableMount.getCapacity();
    }

    public boolean isReadOnly(String path) {
        return this.writableMount == null;
    }

    public boolean exists(String path) throws FileSystemException {
        path = this.toLocal(path);
        try {
            return this.mount.exists(path);
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    private String toLocal(String path) {
        return FileSystem.toLocal(path, this.location);
    }

    private FileSystemException localExceptionOf(@Nullable String localPath, @Nonnull IOException e) {
        if (!this.location.isEmpty() && e instanceof FileOperationException) {
            FileOperationException ex = (FileOperationException) e;
            if (ex.getFilename() != null) {
                return this.localExceptionOf(ex.getFilename(), ex.getMessage());
            }
        }

        if (e instanceof java.nio.file.FileSystemException) {
            // This error will contain the absolute path, leaking information about where MC is installed. We drop that,
            // just taking the reason. We assume that the error refers to the input path.
            String message = ((java.nio.file.FileSystemException) e).getReason()
                                                                    .trim();
            return localPath == null ? new FileSystemException(message) : this.localExceptionOf(localPath, message);
        }

        return new FileSystemException(e.getMessage());
    }

    private FileSystemException localExceptionOf(String path, String message) {
        if (!this.location.isEmpty()) {
            path = path.isEmpty() ? this.location : this.location + "/" + path;
        }
        return exceptionOf(path, message);
    }

    private static FileSystemException exceptionOf(String path, String message) {
        return new FileSystemException("/" + path + ": " + message);
    }

    public boolean isDirectory(String path) throws FileSystemException {
        path = this.toLocal(path);
        try {
            return this.mount.exists(path) && this.mount.isDirectory(path);
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public void list(String path, List<String> contents) throws FileSystemException {
        path = this.toLocal(path);
        try {
            if (!this.mount.exists(path) || !this.mount.isDirectory(path)) {
                throw this.localExceptionOf(path, "Not a directory");
            }

            this.mount.list(path, contents);
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public long getSize(String path) throws FileSystemException {
        path = this.toLocal(path);
        try {
            if (!this.mount.exists(path)) {
                throw this.localExceptionOf(path, "No such file");
            }
            return this.mount.isDirectory(path) ? 0 : this.mount.getSize(path);
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    @Nonnull
    public BasicFileAttributes getAttributes(String path) throws FileSystemException {
        path = this.toLocal(path);
        try {
            if (!this.mount.exists(path)) {
                throw this.localExceptionOf(path, "No such file");
            }
            return this.mount.getAttributes(path);
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public ReadableByteChannel openForRead(String path) throws FileSystemException {
        path = this.toLocal(path);
        try {
            if (this.mount.exists(path) && !this.mount.isDirectory(path)) {
                return this.mount.openForRead(path);
            } else {
                throw this.localExceptionOf(path, "No such file");
            }
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public void makeDirectory(String path) throws FileSystemException {
        if (this.writableMount == null) {
            throw exceptionOf(path, "Access denied");
        }

        path = this.toLocal(path);
        try {
            if (this.mount.exists(path)) {
                if (!this.mount.isDirectory(path)) {
                    throw this.localExceptionOf(path, "File exists");
                }
            } else {
                this.writableMount.makeDirectory(path);
            }
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public void delete(String path) throws FileSystemException {
        if (this.writableMount == null) {
            throw exceptionOf(path, "Access denied");
        }

        path = this.toLocal(path);
        try {
            if (this.mount.exists(path)) {
                this.writableMount.delete(path);
            }
        } catch (AccessDeniedException e) {
            throw new FileSystemException("Access denied");
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public WritableByteChannel openForWrite(String path) throws FileSystemException {
        if (this.writableMount == null) {
            throw exceptionOf(path, "Access denied");
        }

        path = this.toLocal(path);
        try {
            if (this.mount.exists(path) && this.mount.isDirectory(path)) {
                throw this.localExceptionOf(path, "Cannot write to directory");
            } else {
                if (!path.isEmpty()) {
                    String dir = FileSystem.getDirectory(path);
                    if (!dir.isEmpty() && !this.mount.exists(path)) {
                        this.writableMount.makeDirectory(dir);
                    }
                }
                return this.writableMount.openForWrite(path);
            }
        } catch (AccessDeniedException e) {
            throw new FileSystemException("Access denied");
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }

    public WritableByteChannel openForAppend(String path) throws FileSystemException {
        if (this.writableMount == null) {
            throw exceptionOf(path, "Access denied");
        }

        path = this.toLocal(path);
        try {
            if (!this.mount.exists(path)) {
                if (!path.isEmpty()) {
                    String dir = FileSystem.getDirectory(path);
                    if (!dir.isEmpty() && !this.mount.exists(path)) {
                        this.writableMount.makeDirectory(dir);
                    }
                }
                return this.writableMount.openForWrite(path);
            } else if (this.mount.isDirectory(path)) {
                throw this.localExceptionOf(path, "Cannot write to directory");
            } else {
                return this.writableMount.openForAppend(path);
            }
        } catch (AccessDeniedException e) {
            throw new FileSystemException("Access denied");
        } catch (IOException e) {
            throw this.localExceptionOf(path, e);
        }
    }
}
