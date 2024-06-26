// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static dan200.computercraft.api.filesystem.MountConstants.EPOCH;
import static dan200.computercraft.api.filesystem.MountConstants.NO_SUCH_FILE;

/**
 * A mount which reads zip/jar files.
 */
public final class JarMount extends ArchiveMount<JarMount.FileEntry> implements Closeable {
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
        var root = this.root = new FileEntry();
        var zipEntries = zip.entries();
        while (zipEntries.hasMoreElements()) {
            var entry = zipEntries.nextElement();

            var localPath = getLocalPath(entry.getName(), subPath);
            if (localPath == null) continue;

            getOrCreateChild(root, localPath, x -> new FileEntry()).setup(entry);
        }
    }

    @Override
    protected long getFileSize(String path, FileEntry file) throws FileOperationException {
        if (file.zipEntry == null) throw new FileOperationException(path, NO_SUCH_FILE);
        return file.zipEntry.getSize();
    }

    @Override
    protected byte[] getFileContents(String path, FileEntry file) throws FileOperationException {
        if (file.zipEntry == null) throw new FileOperationException(path, NO_SUCH_FILE);

        try (var stream = zip.getInputStream(file.zipEntry)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            // Mask other IO exceptions as a non-existent file.
            throw new FileOperationException(path, NO_SUCH_FILE);
        }
    }

    @Override
    protected BasicFileAttributes getAttributes(String path, FileEntry file) throws IOException {
        return file.zipEntry == null ? super.getAttributes(path, file) : new FileAttributes(
            file.isDirectory(), getSize(path, file), orEpoch(file.zipEntry.getCreationTime()), orEpoch(file.zipEntry.getLastModifiedTime())
        );
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    protected static final class FileEntry extends ArchiveMount.FileEntry<FileEntry> {
        @Nullable
        ZipEntry zipEntry;

        void setup(ZipEntry entry) {
            zipEntry = entry;
            if (children == null && entry.isDirectory()) children = new HashMap<>(0);
        }
    }

    private static FileTime orEpoch(@Nullable FileTime time) {
        return time == null ? EPOCH : time;
    }
}
