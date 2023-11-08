// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import com.google.common.base.Joiner;
import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static dan200.computercraft.api.filesystem.MountConstants.*;

/**
 * A {@link Mount} implementation which provides read-only access to a directory.
 */
public class FileMount implements Mount {
    protected final Path root;

    public FileMount(Path root) {
        this.root = root;
    }

    /**
     * Resolve a mount-relative path to one on the file system.
     *
     * @param path The path to resolve.
     * @return The resolved path.
     */
    protected Path resolvePath(String path) {
        return root.resolve(path);
    }

    protected boolean created() {
        return Files.exists(root);
    }

    @Override
    public boolean exists(String path) {
        return path.isEmpty() || Files.exists(resolvePath(path));
    }

    @Override
    public boolean isDirectory(String path) {
        return path.isEmpty() || Files.isDirectory(resolvePath(path));
    }

    @Override
    public void list(String path, List<String> contents) throws FileOperationException {
        if (path.isEmpty() && !created()) return;

        try (var stream = Files.newDirectoryStream(resolvePath(path))) {
            stream.forEach(x -> contents.add(x.getFileName().toString()));
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    @Override
    public long getSize(String path) throws FileOperationException {
        var attributes = getAttributes(path);
        return attributes.isDirectory() ? 0 : attributes.size();
    }

    @Override
    public BasicFileAttributes getAttributes(String path) throws FileOperationException {
        if (path.isEmpty() && !created()) return new FileAttributes(true, 0);

        try {
            return Files.readAttributes(resolvePath(path), BasicFileAttributes.class);
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    @Override
    public SeekableByteChannel openForRead(String path) throws FileOperationException {
        var file = resolvePath(path);
        if (!Files.isRegularFile(file)) {
            throw new FileOperationException(path, Files.exists(file) ? NOT_A_FILE : NO_SUCH_FILE);
        }

        try {
            return Files.newByteChannel(file, READ_OPTIONS);
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    /**
     * Remap a {@link IOException} to a friendlier {@link FileOperationException}.
     *
     * @param fallbackPath The path currently being operated on. This is used in errors when we cannot determine a more accurate path.
     * @param exn          The exception that occurred.
     * @return The wrapped exception.
     */
    protected FileOperationException remapException(String fallbackPath, IOException exn) {
        return exn instanceof FileSystemException fsExn
            ? remapException(fallbackPath, fsExn)
            : new FileOperationException(fallbackPath, exn.getMessage() == null ? ACCESS_DENIED : exn.getMessage());
    }

    /**
     * Remap a {@link FileSystemException} to a friendlier {@link FileOperationException}, attempting to remap the path
     * provided.
     *
     * @param fallbackPath The path currently being operated on. This is used in errors when we cannot determine a more accurate path.
     * @param exn          The exception that occurred.
     * @return The wrapped exception.
     */
    protected FileOperationException remapException(String fallbackPath, FileSystemException exn) {
        var reason = MountHelpers.getReason(exn);

        var failedFile = exn.getFile();
        if (failedFile == null) return new FileOperationException(fallbackPath, reason);

        var failedPath = Path.of(failedFile);
        return failedPath.startsWith(root)
            ? new FileOperationException(Joiner.on('/').join(root.relativize(failedPath)), reason)
            : new FileOperationException(fallbackPath, reason);
    }
}
