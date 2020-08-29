/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import dan200.computercraft.api.filesystem.IFileSystem;

public class FileSystemWrapperMount implements IFileSystem {
    private final FileSystem m_filesystem;

    public FileSystemWrapperMount(FileSystem filesystem) {
        this.m_filesystem = filesystem;
    }

    @Override
    public void makeDirectory(@Nonnull String path) throws IOException {
        try {
            this.m_filesystem.makeDir(path);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void delete(@Nonnull String path) throws IOException {
        try {
            this.m_filesystem.delete(path);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForWrite(@Nonnull String path) throws IOException {
        try {
            return this.m_filesystem.openForWrite(path, false, Function.identity())
                                    .get();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public OutputStream openForWrite(@Nonnull String path) throws IOException {
        return Channels.newOutputStream(this.openChannelForWrite(path));
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForAppend(@Nonnull String path) throws IOException {
        try {
            return this.m_filesystem.openForWrite(path, true, Function.identity())
                                    .get();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public OutputStream openForAppend(@Nonnull String path) throws IOException {
        return Channels.newOutputStream(this.openChannelForAppend(path));
    }

    @Override
    public long getRemainingSpace() throws IOException {
        try {
            return this.m_filesystem.getFreeSpace("/");
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public boolean exists(@Nonnull String path) throws IOException {
        try {
            return this.m_filesystem.exists(path);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public boolean isDirectory(@Nonnull String path) throws IOException {
        try {
            return this.m_filesystem.exists(path);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        try {
            Collections.addAll(contents, this.m_filesystem.list(path));
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        try {
            return this.m_filesystem.getSize(path);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead(@Nonnull String path) throws IOException {
        try {
            // FIXME: Think of a better way of implementing this, so closing this will close on the computer.
            return this.m_filesystem.openForRead(path, Function.identity())
                                    .get();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead(@Nonnull String path) throws IOException {
        return Channels.newInputStream(this.openChannelForRead(path));
    }

    @Override
    public String combine(String path, String child) {
        return this.m_filesystem.combine(path, child);
    }

    @Override
    public void copy(String from, String to) throws IOException {
        try {
            this.m_filesystem.copy(from, to);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void move(String from, String to) throws IOException {
        try {
            this.m_filesystem.move(from, to);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }
}
