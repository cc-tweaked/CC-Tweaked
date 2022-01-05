/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.filesystem;

import java.io.IOException;

/**
 * Provides a mount of the entire computer's file system.
 *
 * This exists for use by various APIs - one should not attempt to mount it.
 */
public interface IFileSystem extends IWritableMount
{
    /**
     * Combine two paths together, reducing them into a normalised form.
     *
     * @param path  The main path.
     * @param child The path to append.
     * @return The combined, normalised path.
     */
    String combine( String path, String child );

    /**
     * Copy files from one location to another.
     *
     * @param from The location to copy from.
     * @param to   The location to copy to. This should not exist.
     * @throws IOException If the copy failed.
     */
    void copy( String from, String to ) throws IOException;

    /**
     * Move files from one location to another.
     *
     * @param from The location to move from.
     * @param to   The location to move to. This should not exist.
     * @throws IOException If the move failed.
     */
    void move( String from, String to ) throws IOException;
}
