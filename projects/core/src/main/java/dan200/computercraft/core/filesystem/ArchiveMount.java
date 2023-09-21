// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.TimeUnit;

/**
 * An abstract mount based on some archive of files, such as a Zip or Minecraft's resources.
 * <p>
 * We assume that we cannot create {@link SeekableByteChannel}s directly from the archive, and so maintain a (shared)
 * cache of recently read files and their contents.
 *
 * @param <T> The type of file.
 */
public abstract class ArchiveMount<T extends ArchiveMount.FileEntry<T>> extends AbstractInMemoryMount<T> {
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

    @Override
    protected final long getSize(T file) throws IOException {
        if (file.size != -1) return file.size;
        if (file.isDirectory()) return file.size = 0;

        var contents = CONTENTS_CACHE.getIfPresent(file);
        return file.size = contents != null ? contents.length : getFileSize(file);
    }

    /**
     * Get the size of the file by reading it (or its metadata) from disk.
     *
     * @param file The file to get the size of.
     * @return The file's size.
     * @throws IOException If the size could not be computed.
     */
    protected long getFileSize(T file) throws IOException {
        return getContents(file).length;
    }

    @Override
    protected final SeekableByteChannel openForRead(T file) throws IOException {
        return new ArrayByteChannel(getContents(file));
    }

    private byte[] getContents(T file) throws IOException {
        var cachedContents = CONTENTS_CACHE.getIfPresent(file);
        if (cachedContents != null) return cachedContents;

        var contents = getFileContents(file);
        CONTENTS_CACHE.put(file, contents);
        return contents;
    }

    /**
     * Read the entirety of a file into memory.
     *
     * @param file The file to read into memory. This will not be a directory.
     * @return The contents of the file.
     */
    protected abstract byte[] getFileContents(T file) throws IOException;

    protected static class FileEntry<T extends FileEntry<T>> extends AbstractInMemoryMount.FileEntry<T> {
        long size = -1;

        protected FileEntry(String path) {
            super(path);
        }
    }
}
