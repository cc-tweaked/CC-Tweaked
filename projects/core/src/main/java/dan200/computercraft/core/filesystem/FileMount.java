/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A {@link Mount} implementation which provides read-only access to a directory.
 */
public class FileMount implements Mount {
    private static final Set<OpenOption> READ_OPTIONS = Collections.singleton(StandardOpenOption.READ);

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
        if (!Files.isRegularFile(file)) throw new FileOperationException(path, "No such file");

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
            : new FileOperationException(fallbackPath, exn.getMessage() == null ? "Operation failed" : exn.getMessage());
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
        var reason = getReason(exn);

        var failedFile = exn.getFile();
        if (failedFile == null) return new FileOperationException(fallbackPath, reason);

        var failedPath = Path.of(failedFile);
        return failedPath.startsWith(root)
            ? new FileOperationException(root.relativize(failedPath).toString(), reason)
            : new FileOperationException(fallbackPath, reason);
    }

    /**
     * Get the user-friendly reason for a {@link FileSystemException}.
     *
     * @param exn The exception that occurred.
     * @return The friendly reason for this exception.
     */
    protected String getReason(FileSystemException exn) {
        if (exn instanceof FileAlreadyExistsException) return "File exists";
        if (exn instanceof NoSuchFileException) return "No such file";
        if (exn instanceof NotDirectoryException) return "Not a directory";
        if (exn instanceof AccessDeniedException) return "Access denied";

        var reason = exn.getReason();
        return reason != null ? reason.trim() : "Operation failed";
    }
}
