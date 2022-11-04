/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.filesystem;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/**
 * A simple version of {@link BasicFileAttributes}, which provides what information a {@link IMount} already exposes.
 *
 * @param isDirectory Whether this filesystem entry is a directory.
 * @param size        The size of the file.
 */
record FileAttributes(boolean isDirectory, long size) implements BasicFileAttributes {
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

    @Override
    public Object fileKey() {
        return null;
    }
}
