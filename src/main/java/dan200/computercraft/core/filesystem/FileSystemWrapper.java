/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

/**
 * An alternative closeable implementation that will free up resources in the filesystem.
 *
 * The {@link FileSystem} maps weak references of this to its underlying object. If the wrapper has been disposed of (say, the Lua object referencing it has
 * gone), then the wrapped object will be closed by the filesystem.
 *
 * Closing this will stop the filesystem tracking it, reducing the current descriptor count.
 *
 * In an ideal world, we'd just wrap the closeable. However, as we do some {@code instanceof} checks on the stream, it's not really possible as it'd require
 * numerous instances.
 *
 * @param <T> The type of writer or channel to wrap.
 */
public class FileSystemWrapper<T extends Closeable> implements Closeable {
    final WeakReference<FileSystemWrapper<?>> self;
    private final FileSystem fileSystem;
    private final ChannelWrapper<T> closeable;

    FileSystemWrapper(FileSystem fileSystem, ChannelWrapper<T> closeable, ReferenceQueue<FileSystemWrapper<?>> queue) {
        this.fileSystem = fileSystem;
        this.closeable = closeable;
        this.self = new WeakReference<>(this, queue);
    }

    @Override
    public void close() throws IOException {
        this.fileSystem.removeFile(this);
        this.closeable.close();
    }

    @Nonnull
    public T get() {
        return this.closeable.get();
    }
}
