// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.filesystem;

import javax.annotation.Nullable;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/**
 * A simple version of {@link BasicFileAttributes}, which provides what information a {@link Mount} already exposes.
 *
 * @param isDirectory Whether this filesystem entry is a directory.
 * @param size        The size of the file.
 */
public record FileAttributes(boolean isDirectory, long size) implements BasicFileAttributes {
    private static final FileTime EPOCH = FileTime.from(Instant.EPOCH);

    @Override
    public FileTime lastModifiedTime() {
        return EPOCH;
    }

    @Override
    public FileTime lastAccessTime() {
        return EPOCH;
    }

    @Override
    public FileTime creationTime() {
        return EPOCH;
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
