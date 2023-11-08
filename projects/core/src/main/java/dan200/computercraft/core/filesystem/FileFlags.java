// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.WritableMount;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import static dan200.computercraft.api.filesystem.MountConstants.UNSUPPORTED_MODE;

/**
 * Tracks the {@link OpenOption}s passed to {@link WritableMount#openFile(String, Set)}.
 *
 * @param read     Whether this file was opened for reading. ({@link StandardOpenOption#READ})
 * @param write    Whether this file was opened for writing. ({@link StandardOpenOption#WRITE})
 * @param truncate Whether to truncate this file when opening. ({@link StandardOpenOption#TRUNCATE_EXISTING})
 * @param create   Whether to create the file if it does not exist. ({@link StandardOpenOption#CREATE})
 * @param append   Whether this file was opened for appending. ({@link StandardOpenOption#APPEND})
 */
record FileFlags(boolean read, boolean write, boolean truncate, boolean create, boolean append) {
    public static FileFlags of(Set<OpenOption> options) throws IOException {
        boolean read = false, write = false, truncate = false, create = false, append = false;
        for (var option : options) {
            if (!(option instanceof StandardOpenOption stdOption)) throw new IOException(UNSUPPORTED_MODE);
            switch (stdOption) {
                case READ -> read = true;
                case WRITE -> write = true;
                case APPEND -> write = append = true;
                case TRUNCATE_EXISTING -> truncate = true;
                case CREATE -> create = true;
                case CREATE_NEW, DELETE_ON_CLOSE, SPARSE, SYNC, DSYNC -> throw new IOException(UNSUPPORTED_MODE);
            }
        }

        // Quick safety check that we've been given something reasonable.
        if (!read && !write) read = true;
        if (read && append) throw new IllegalArgumentException("Cannot use READ and APPEND");
        if (append && truncate) throw new IllegalArgumentException("Cannot use APPEND and TRUNCATE_EXISTING");

        return new FileFlags(read, write, truncate, create, append);
    }
}
