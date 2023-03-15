// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.core.util.Nullability;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A mount which reads zip/jar files.
 */
public class JarMount extends ArchiveMount<JarMount.FileEntry> implements Closeable {
    private final ZipFile zip;

    public JarMount(File jarFile, String subPath) throws IOException {
        if (!jarFile.exists() || jarFile.isDirectory()) throw new FileNotFoundException("Cannot find " + jarFile);

        // Open the zip file
        try {
            zip = new ZipFile(jarFile);
        } catch (IOException e) {
            throw new IOException("Error loading zip file", e);
        }

        // Ensure the root entry exists.
        if (zip.getEntry(subPath) == null) {
            zip.close();
            throw new FileNotFoundException("Zip does not contain path");
        }

        // Read in all the entries
        root = new FileEntry("");
        var zipEntries = zip.entries();
        while (zipEntries.hasMoreElements()) {
            var entry = zipEntries.nextElement();

            var entryPath = entry.getName();
            if (!entryPath.startsWith(subPath)) continue;

            var localPath = FileSystem.toLocal(entryPath, subPath);
            create(entry, localPath);
        }
    }

    private void create(ZipEntry entry, String localPath) {
        var lastEntry = Nullability.assertNonNull(root);

        var lastIndex = 0;
        while (lastIndex < localPath.length()) {
            var nextIndex = localPath.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = localPath.length();

            var part = localPath.substring(lastIndex, nextIndex);
            if (lastEntry.children == null) lastEntry.children = new HashMap<>(0);

            var nextEntry = lastEntry.children.get(part);
            if (nextEntry == null || !nextEntry.isDirectory()) {
                lastEntry.children.put(part, nextEntry = new FileEntry(localPath.substring(0, nextIndex)));
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }

        lastEntry.setup(entry);
    }

    @Override
    protected long getSize(FileEntry file) throws FileOperationException {
        if (file.zipEntry == null) throw new FileOperationException(file.path, NO_SUCH_FILE);
        return file.zipEntry.getSize();
    }

    @Override
    protected byte[] getContents(FileEntry file) throws FileOperationException {
        if (file.zipEntry == null) throw new FileOperationException(file.path, NO_SUCH_FILE);

        try (var stream = zip.getInputStream(file.zipEntry)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            // Mask other IO exceptions as a non-existent file.
            throw new FileOperationException(file.path, NO_SUCH_FILE);
        }
    }

    @Override
    public BasicFileAttributes getAttributes(FileEntry file) throws FileOperationException {
        if (file.zipEntry == null) throw new FileOperationException(file.path, NO_SUCH_FILE);
        return new ZipEntryAttributes(file.zipEntry);
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    protected static class FileEntry extends ArchiveMount.FileEntry<FileEntry> {
        @Nullable
        ZipEntry zipEntry;

        protected FileEntry(String path) {
            super(path);
        }

        void setup(ZipEntry entry) {
            zipEntry = entry;
            size = entry.getSize();
            if (children == null && entry.isDirectory()) children = new HashMap<>(0);
        }
    }

    private static class ZipEntryAttributes implements BasicFileAttributes {
        private final ZipEntry entry;

        ZipEntryAttributes(ZipEntry entry) {
            this.entry = entry;
        }

        @Override
        public FileTime lastModifiedTime() {
            return orEpoch(entry.getLastModifiedTime());
        }

        @Override
        public FileTime lastAccessTime() {
            return orEpoch(entry.getLastAccessTime());
        }

        @Override
        public FileTime creationTime() {
            var time = entry.getCreationTime();
            return time == null ? lastModifiedTime() : time;
        }

        @Override
        public boolean isRegularFile() {
            return !entry.isDirectory();
        }

        @Override
        public boolean isDirectory() {
            return entry.isDirectory();
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return entry.getSize();
        }

        @Nullable
        @Override
        public Object fileKey() {
            return null;
        }

        private static final FileTime EPOCH = FileTime.from(Instant.EPOCH);

        private static FileTime orEpoch(@Nullable FileTime time) {
            return time == null ? EPOCH : time;
        }
    }
}
