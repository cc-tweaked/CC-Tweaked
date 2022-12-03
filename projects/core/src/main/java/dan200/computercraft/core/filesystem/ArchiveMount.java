/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An abstract mount based on some archive of files, such as a Zip or Minecraft's resources.
 *
 * @param <T> The type of file.
 */
public abstract class ArchiveMount<T extends ArchiveMount.FileEntry<T>> implements Mount {
    protected static final String NO_SUCH_FILE = "No such file";
    /**
     * Limit the entire cache to 64MiB.
     */
    private static final int MAX_CACHE_SIZE = 64 << 20;

    /**
     * We maintain a cache of the contents of all files in the mount. This allows us to allow
     * seeking within ROM files, and reduces the amount we need to access disk for computer startup.
     */
    private static final Cache<FileEntry<?>, byte[]> CONTENTS_CACHE = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .expireAfterAccess(60, TimeUnit.SECONDS)
        .maximumWeight(MAX_CACHE_SIZE)
        .weakKeys()
        .<FileEntry<?>, byte[]>weigher((k, v) -> v.length)
        .build();

    @Nullable
    protected T root;

    private @Nullable T get(String path) {
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
        if (file == null || !file.isDirectory()) throw new FileOperationException(path, "Not a directory");

        file.list(contents);
    }

    @Override
    public final long getSize(String path) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);
        return getCachedSize(file);
    }

    private long getCachedSize(T file) throws IOException {
        if (file.size != -1) return file.size;
        if (file.isDirectory()) return file.size = 0;

        var contents = CONTENTS_CACHE.getIfPresent(file);
        if (contents != null) return file.size = contents.length;

        return file.size = getSize(file);
    }

    /**
     * Get the size of a file.
     * <p>
     * This should only be called once per file, as the result is cached in {@link #getSize(String)}.
     *
     * @param file The file to compute the size of.
     * @return The size of the file.
     * @throws IOException If the size could not be read.
     */
    protected abstract long getSize(T file) throws IOException;

    @Override
    public SeekableByteChannel openForRead(String path) throws IOException {
        var file = get(path);
        if (file == null || file.isDirectory()) throw new FileOperationException(path, NO_SUCH_FILE);

        var cachedContents = CONTENTS_CACHE.getIfPresent(file);
        if (cachedContents != null) return new ArrayByteChannel(cachedContents);

        var contents = getContents(file);
        CONTENTS_CACHE.put(file, contents);
        return new ArrayByteChannel(contents);
    }

    /**
     * Read the entirety of a file into memory.
     *
     * @param file The file to read into memory.
     * @return The contents of the file.
     */
    protected abstract byte[] getContents(T file) throws IOException;

    @Override
    public final BasicFileAttributes getAttributes(String path) throws IOException {
        var file = get(path);
        if (file == null) throw new FileOperationException(path, NO_SUCH_FILE);

        return getAttributes(file);
    }

    /**
     * Get all attributes of the file.
     *
     * @param file The file to compute attributes for.
     * @return The file's attributes.
     * @throws IOException If the attributes could not be read.
     */
    protected BasicFileAttributes getAttributes(T file) throws IOException {
        return new FileAttributes(file.isDirectory(), getCachedSize(file));
    }

    protected static class FileEntry<T extends ArchiveMount.FileEntry<T>> {
        @Nullable
        public Map<String, T> children;

        long size = -1;

        protected boolean isDirectory() {
            return children != null;
        }

        protected void list(List<String> contents) {
            if (children != null) contents.addAll(children.keySet());
        }
    }
}
