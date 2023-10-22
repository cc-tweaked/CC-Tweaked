// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;

import java.nio.file.FileSystemException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

/**
 * Useful constants and helper functions for working with mounts.
 */
public final class MountHelpers {
    /**
     * A {@link FileTime} set to the Unix EPOCH, intended for {@link BasicFileAttributes}'s file times.
     */
    public static final FileTime EPOCH = FileTime.from(Instant.EPOCH);

    /**
     * The minimum size of a file for file {@linkplain WritableMount#getCapacity() capacity calculations}.
     */
    public static final long MINIMUM_FILE_SIZE = 500;

    /**
     * The error message used when the file does not exist.
     */
    public static final String NO_SUCH_FILE = "No such file";

    /**
     * The error message used when trying to use a file as a directory (for instance when
     * {@linkplain Mount#list(String, List) listing its contents}).
     */
    public static final String NOT_A_DIRECTORY = "Not a directory";

    /**
     * The error message used when trying to use a directory as a file (for instance when
     * {@linkplain Mount#openForRead(String) opening for reading}).
     */
    public static final String NOT_A_FILE = "Not a file";

    /**
     * The error message used when attempting to modify a read-only file or mount.
     */
    public static final String ACCESS_DENIED = "Access denied";

    /**
     * The error message used when trying to overwrite a file (for instance when
     * {@linkplain WritableMount#rename(String, String) renaming files} or {@linkplain WritableMount#makeDirectory(String)
     * creating directories}).
     */
    public static final String FILE_EXISTS = "File exists";

    /**
     * The error message used when trying to {@linkplain WritableMount#openForWrite(String) opening a directory to read}.
     */
    public static final String CANNOT_WRITE_TO_DIRECTORY = "Cannot write to directory";

    /**
     * The error message used when the mount runs out of space.
     */
    public static final String OUT_OF_SPACE = "Out of space";

    private MountHelpers() {
    }

    /**
     * Get the user-friendly reason for a {@link java.nio.file.FileSystemException}.
     *
     * @param exn The exception that occurred.
     * @return The friendly reason for this exception.
     */
    public static String getReason(FileSystemException exn) {
        if (exn instanceof FileAlreadyExistsException) return FILE_EXISTS;
        if (exn instanceof NoSuchFileException) return NO_SUCH_FILE;
        if (exn instanceof NotDirectoryException) return NOT_A_DIRECTORY;
        if (exn instanceof AccessDeniedException) return ACCESS_DENIED;

        var reason = exn.getReason();
        return reason != null ? reason.trim() : "Operation failed";
    }
}
