/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Wraps a {@link Mount} into a read-only {@link WritableMount}.
 *
 * @param mount The original read-only mount we're wrapping.
 */
public record ReadOnlyWritableMount(Mount mount) implements WritableMount {
    @Override
    public boolean exists(String path) throws IOException {
        return mount.exists(path);
    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        return mount.isDirectory(path);
    }

    @Override
    public void list(String path, List<String> contents) throws IOException {
        mount.list(path, contents);
    }

    @Override
    public long getSize(String path) throws IOException {
        return mount.getSize(path);
    }

    @Override
    public SeekableByteChannel openForRead(String path) throws IOException {
        return mount.openForRead(path);
    }

    @Override
    public BasicFileAttributes getAttributes(String path) throws IOException {
        return mount.getAttributes(path);
    }

    @Override
    public void makeDirectory(String path) throws IOException {
        throw new FileOperationException(path, "Access denied");
    }

    @Override
    public void delete(String path) throws IOException {
        throw new FileOperationException(path, "Access denied");
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        throw new FileOperationException(source, "Access denied");
    }

    @Override
    public SeekableByteChannel openForWrite(String path) throws IOException {
        throw new FileOperationException(path, "Access denied");
    }

    @Override
    public SeekableByteChannel openForAppend(String path) throws IOException {
        throw new FileOperationException(path, "Access denied");
    }

    @Override
    public long getRemainingSpace() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isReadOnly(String path) {
        return true;
    }
}
