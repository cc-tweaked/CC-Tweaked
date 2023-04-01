// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.filesystem;

import dan200.computercraft.api.peripheral.IComputerAccess;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * Represents a part of a virtual filesystem that can be mounted onto a computer using {@link IComputerAccess#mount(String, Mount)}
 * or {@link IComputerAccess#mountWritable(String, WritableMount)}, that can also be written to.
 * <p>
 * Typically you will not need to implement this interface yourself, and can use the factory methods from the
 * {@linkplain dan200.computercraft.api.ComputerCraftAPI the main ComputerCraft API}.
 *
 * @see IComputerAccess#mount(String, Mount)
 * @see IComputerAccess#mountWritable(String, WritableMount)
 * @see Mount
 */
public interface WritableMount extends Mount {
    /**
     * Creates a directory at a given path inside the virtual file system.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/mynewprograms".
     * @throws IOException If the directory already exists or could not be created.
     */
    void makeDirectory(String path) throws IOException;

    /**
     * Deletes a directory at a given path inside the virtual file system. If the file does not exist, this method
     * should do nothing.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myoldprograms".
     * @throws IOException If the file does not exist or could not be deleted.
     */
    void delete(String path) throws IOException;

    /**
     * Rename a file or directory, moving it from one path to another.
     * <p>
     * The destination path should not exist. The parent of the destination should exist and be a directory. If source
     * and destination are the same, this method should do nothing.
     *
     * @param source The source file or directory to move.
     * @param dest   The destination path.
     */
    void rename(String source, String dest) throws IOException;

    /**
     * Opens a file with a given path, and returns an {@link OutputStream} for writing to it.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram".
     * @return A stream for writing to.
     * @throws IOException If the file could not be opened for writing.
     */
    SeekableByteChannel openForWrite(String path) throws IOException;

    /**
     * Opens a file with a given path, and returns an {@link OutputStream} for appending to it.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram".
     * @return A stream for writing to.
     * @throws IOException If the file could not be opened for writing.
     */
    SeekableByteChannel openForAppend(String path) throws IOException;

    /**
     * Get the amount of free space on the mount, in bytes. You should decrease this value as the user writes to the
     * mount, and write operations should fail once it reaches zero.
     *
     * @return The amount of free space, in bytes.
     * @throws IOException If the remaining space could not be computed.
     */
    long getRemainingSpace() throws IOException;

    /**
     * Get the capacity of this mount. This should be equal to the size of all files/directories on this mount, minus
     * the {@link #getRemainingSpace()}.
     *
     * @return The capacity of this mount, in bytes.
     */
    long getCapacity();

    /**
     * Returns whether a file with a given path is read-only or not.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprograms".
     * @return If the file exists and is read-only.
     * @throws IOException If an error occurs when checking whether the file is read-only.
     */
    default boolean isReadOnly(String path) throws IOException {
        return false;
    }
}
