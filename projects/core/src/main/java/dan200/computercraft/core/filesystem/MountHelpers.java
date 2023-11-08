// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.MountConstants;

import java.nio.file.FileSystemException;
import java.nio.file.*;

/**
 * Useful constants and helper functions for working with mounts.
 */
public final class MountHelpers {
    private MountHelpers() {
    }

    /**
     * Get the user-friendly reason for a {@link java.nio.file.FileSystemException}.
     *
     * @param exn The exception that occurred.
     * @return The friendly reason for this exception.
     */
    public static String getReason(FileSystemException exn) {
        if (exn instanceof FileAlreadyExistsException) return MountConstants.FILE_EXISTS;
        if (exn instanceof NoSuchFileException) return MountConstants.NO_SUCH_FILE;
        if (exn instanceof NotDirectoryException) return MountConstants.NOT_A_DIRECTORY;
        if (exn instanceof AccessDeniedException) return MountConstants.ACCESS_DENIED;

        var reason = exn.getReason();
        return reason != null ? reason.trim() : "Operation failed";
    }
}
