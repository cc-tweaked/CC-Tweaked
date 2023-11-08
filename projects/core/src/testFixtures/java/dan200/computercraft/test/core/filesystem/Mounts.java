// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

/**
 * Utility functions for working with mounts.
 */
public final class Mounts {
    private Mounts() {
    }

    /**
     * Write a file to this mount.
     *
     * @param mount    The mount to modify.
     * @param path     The path to write to.
     * @param contents The contents of this path.
     * @throws IOException If writing fails.
     */
    public static void writeFile(WritableMount mount, String path, String contents) throws IOException {
        try (var handle = Channels.newWriter(mount.openFile(path, MountConstants.WRITE_OPTIONS), StandardCharsets.UTF_8)) {
            handle.write(contents);
        }
    }
}
