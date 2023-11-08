// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dan200.computercraft.api.filesystem.MountConstants.*;

/**
 * An abstract mount which stores its file tree in memory.
 *
 * @param <T> The type of file.
 */
public abstract class AbstractInMemoryMount<T extends AbstractInMemoryMount.FileEntry<T>> implements Mount {
    @Nullable
    protected T root;

    protected final @Nullable T get(String path) {
        var lastEntry = root;
        var lastIndex = 0;

        while (lastEntry != null && lastIndex < path.length()) {
            var nextIndex = path.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = path.length();

            lastEntry = lastEntry.children == null ? null : lastEntry.children.get(path.substring(lastIndex, nextIndex));
            lastIndex = nextIndex + 1;
        }

        return lastEntry;
    }

    @Override
    public final boolean exists(String path) {
        return get(path) != null;
    }

    @Override
    public final boolean isDirectory(String path) {
        var file = get(path);
        return file != null && file.isDirectory();
    }

    @Override
    public final void list(String path, List<String> contents) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);
        if (file.children == null) throw new FileOperationException(path, NOT_A_DIRECTORY);

        contents.addAll(file.children.keySet());
    }

    @Override
    public final long getSize(String path) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);
        return getSize(path, file);
    }

    /**
     * Get the size of a file.
     *
     * @param path The file path, for error messages.
     * @param file The file to get the size of.
     * @return The size of the file. This should be 0 for directories, and equal to {@code openForRead(_).size()} for files.
     * @throws IOException If the size could not be read.
     */
    protected abstract long getSize(String path, T file) throws IOException;

    @Override
    public final SeekableByteChannel openForRead(String path) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);
        if (file.isDirectory()) throw new FileOperationException(path, NOT_A_FILE);
        return openForRead(path, file);
    }

    /**
     * Open a file for reading.
     *
     * @param path The file path, for error messages.
     * @param file The file to read. This will not be a directory.
     * @return The channel for this file.
     */
    protected abstract SeekableByteChannel openForRead(String path, T file) throws IOException;

    @Override
    public final BasicFileAttributes getAttributes(String path) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);
        return getAttributes(path, file);
    }

    /**
     * Get all attributes of the file.
     *
     * @param path The file path, for error messages.
     * @param file The file to compute attributes for.
     * @return The file's attributes.
     * @throws IOException If the attributes could not be read.
     */
    protected BasicFileAttributes getAttributes(String path, T file) throws IOException {
        return new FileAttributes(file.isDirectory(), getSize(path, file));
    }

    protected T getOrCreateChild(T lastEntry, String localPath, Function<String, T> factory) {
        var lastIndex = 0;
        while (lastIndex < localPath.length()) {
            var nextIndex = localPath.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = localPath.length();

            var part = localPath.substring(lastIndex, nextIndex);
            if (lastEntry.children == null) lastEntry.children = new HashMap<>(0);

            var nextEntry = lastEntry.children.get(part);
            if (nextEntry == null || !nextEntry.isDirectory()) {
                lastEntry.children.put(part, nextEntry = factory.apply(localPath.substring(0, nextIndex)));
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }

        return lastEntry;
    }

    protected static class FileEntry<T extends FileEntry<T>> {
        @Nullable
        public Map<String, T> children;

        public boolean isDirectory() {
            return children != null;
        }
    }
}
