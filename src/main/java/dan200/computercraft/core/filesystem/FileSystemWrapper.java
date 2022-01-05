/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.shared.util.IoUtil;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * An alternative closeable implementation that will free up resources in the filesystem.
 *
 * The {@link FileSystem} maps weak references of this to its underlying object. If the wrapper has been disposed of
 * (say, the Lua object referencing it has gone), then the wrapped object will be closed by the filesystem.
 *
 * Closing this will stop the filesystem tracking it, reducing the current descriptor count.
 *
 * In an ideal world, we'd just wrap the closeable. However, as we do some {@code instanceof} checks
 * on the stream, it's not really possible as it'd require numerous instances.
 *
 * @param <T> The type of writer or channel to wrap.
 */
public class FileSystemWrapper<T extends Closeable> implements TrackingCloseable
{
    private final FileSystem fileSystem;
    final MountWrapper mount;
    private final ChannelWrapper<T> closeable;
    final WeakReference<FileSystemWrapper<?>> self;
    private boolean isOpen = true;

    FileSystemWrapper( FileSystem fileSystem, MountWrapper mount, ChannelWrapper<T> closeable, ReferenceQueue<FileSystemWrapper<?>> queue )
    {
        this.fileSystem = fileSystem;
        this.mount = mount;
        this.closeable = closeable;
        self = new WeakReference<>( this, queue );
    }

    @Override
    public void close() throws IOException
    {
        isOpen = false;
        fileSystem.removeFile( this );
        closeable.close();
    }

    void closeExternally()
    {
        isOpen = false;
        IoUtil.closeQuietly( closeable );
    }

    @Override
    public boolean isOpen()
    {
        return isOpen;
    }

    @Nonnull
    public T get()
    {
        return closeable.get();
    }
}
