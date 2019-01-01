/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * An alternative closeable implementation that will free up resources in the filesystem.
 *
 * In an ideal world, we'd just wrap the closeable. However, as we do some {@code instanceof} checks
 * on the stream, it's not really possible as it'd require numerous instances.
 *
 * @param <T> The stream to wrap.
 */
public class FileSystemWrapper<T extends Closeable> implements Closeable
{
    private final FileSystem fileSystem;
    private final T closeable;
    final WeakReference<FileSystemWrapper<?>> self;

    FileSystemWrapper( FileSystem fileSystem, T closeable, ReferenceQueue<FileSystemWrapper<?>> queue )
    {
        this.fileSystem = fileSystem;
        this.closeable = closeable;
        this.self = new WeakReference<>( this, queue );
    }

    @Override
    public void close() throws IOException
    {
        fileSystem.removeFile( this );
        closeable.close();
    }

    @Nonnull
    public T get()
    {
        return closeable;
    }
}
