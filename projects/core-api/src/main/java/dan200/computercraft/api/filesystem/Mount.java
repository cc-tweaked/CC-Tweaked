// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.filesystem;

import dan200.computercraft.api.peripheral.IComputerAccess;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Represents a read only part of a virtual filesystem that can be mounted onto a computer using
 * {@link IComputerAccess#mount(String, Mount)}.
 * <p>
 * Typically you will not need to implement this interface yourself, and can use the factory methods from the
 * {@linkplain dan200.computercraft.api.ComputerCraftAPI the main ComputerCraft API}.
 *
 * @see IComputerAccess#mount(String, Mount)
 * @see WritableMount
 */
public interface Mount {
    /**
     * Returns whether a file with a given path exists or not.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram"
     * @return If the file exists.
     * @throws IOException If an error occurs when checking the existence of the file.
     */
    boolean exists(String path) throws IOException;

    /**
     * Returns whether a file with a given path is a directory or not.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprograms".
     * @return If the file exists and is a directory
     * @throws IOException If an error occurs when checking whether the file is a directory.
     */
    boolean isDirectory(String path) throws IOException;

    /**
     * Returns the file names of all the files in a directory.
     *
     * @param path     A file path in normalised format, relative to the mount location. ie: "programs/myprograms".
     * @param contents A list of strings. Add all the file names to this list.
     * @throws IOException If the file was not a directory, or could not be listed.
     */
    void list(String path, List<String> contents) throws IOException;

    /**
     * Returns the size of a file with a given path, in bytes.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram".
     * @return The size of the file, in bytes.
     * @throws IOException If the file does not exist, or its size could not be determined.
     */
    long getSize(String path) throws IOException;

    /**
     * Opens a file with a given path, and returns a {@link SeekableByteChannel} representing its contents.
     *
     * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram".
     * @return A channel representing the contents of the file.
     * @throws IOException If the file does not exist, or could not be opened.
     */
    SeekableByteChannel openForRead(String path) throws IOException;

    /**
     * Get attributes about the given file.
     *
     * @param path The path to query.
     * @return File attributes for the given file.
     * @throws IOException If the file does not exist, or attributes could not be fetched.
     */
    default BasicFileAttributes getAttributes(String path) throws IOException {
        if (!exists(path)) throw new FileOperationException(path, "No such file");
        return new FileAttributes(isDirectory(path), getSize(path));
    }
}
