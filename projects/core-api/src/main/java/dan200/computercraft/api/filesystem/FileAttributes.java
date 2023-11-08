// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.filesystem;

import javax.annotation.Nullable;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static dan200.computercraft.api.filesystem.MountConstants.EPOCH;

/**
 * A simple version of {@link BasicFileAttributes}, which provides what information a {@link Mount} already exposes.
 *
 * @param isDirectory      Whether this filesystem entry is a directory.
 * @param size             The size of the file.
 * @param creationTime     The time the file was created.
 * @param lastModifiedTime The time the file was last modified.
 */
public record FileAttributes(
    boolean isDirectory, long size, FileTime creationTime, FileTime lastModifiedTime
) implements BasicFileAttributes {
    /**
     * Create a new {@link FileAttributes} instance with the {@linkplain #creationTime() creation time} and
     * {@linkplain #lastModifiedTime() last modified time} set to the Unix epoch.
     *
     * @param isDirectory Whether the filesystem entry is a directory.
     * @param size        The size of the file.
     */
    public FileAttributes(boolean isDirectory, long size) {
        this(isDirectory, size, EPOCH, EPOCH);
    }

    @Override
    public FileTime lastAccessTime() {
        return lastModifiedTime();
    }

    @Override
    public boolean isRegularFile() {
        return !isDirectory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Nullable
    @Override
    public Object fileKey() {
        return null;
    }
}
