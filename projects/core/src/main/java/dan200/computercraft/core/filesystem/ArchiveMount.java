// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import javax.annotation.Nullable;
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
    protected final long getSize(String path, T file) throws IOException {
        if (file.size != -1) return file.size;
        if (file.isDirectory()) return file.size = 0;

        var contents = CONTENTS_CACHE.getIfPresent(file);
        return file.size = contents != null ? contents.length : getFileSize(path, file);
    }

    /**
     * Get the size of the file by reading it (or its metadata) from disk.
     *
     * @param path The file path, for error messages.
     * @param file The file to get the size of.
     * @return The file's size.
     * @throws IOException If the size could not be computed.
     */
    protected long getFileSize(String path, T file) throws IOException {
        return getContents(path, file).length;
    }

    @Override
    protected final SeekableByteChannel openForRead(String path, T file) throws IOException {
        return new ArrayByteChannel(getContents(path, file));
    }

    private byte[] getContents(String path, T file) throws IOException {
        var cachedContents = CONTENTS_CACHE.getIfPresent(file);
        if (cachedContents != null) return cachedContents;

        var contents = getFileContents(path, file);
        CONTENTS_CACHE.put(file, contents);
        return contents;
    }

    /**
     * Read the entirety of a file into memory.
     *
     * @param path The file path, for error messages.
     * @param file The file to read into memory. This will not be a directory.
     * @return The contents of the file.
     */
    protected abstract byte[] getFileContents(String path, T file) throws IOException;

    /**
     * Convert an absolute path to one relative to {@code root}. If this path is not a child of {@code root}, return
     * {@code null}.
     *
     * @param path The full path.
     * @param root The root directory to be relative to.
     * @return The relativised path, or {@code null}.
     */
    protected static @Nullable String getLocalPath(String path, String root) {
        // Some packs seem to include files not under the root, so drop them immediately.
        if (!path.startsWith(root)) return null;

        if (path.length() == root.length()) return "";

        if (path.charAt(root.length()) != '/') return null;
        return path.substring(root.length() + 1);
    }

    protected static class FileEntry<T extends FileEntry<T>> extends AbstractInMemoryMount.FileEntry<T> {
        long size = -1;
    }
}
