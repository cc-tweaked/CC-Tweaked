// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Location for temporary test files.
 */
public final class TestFiles {
    private static final Path ROOT = Paths.get(System.getProperty("cct.test-files", "build/tmp/testFiles"));

    private TestFiles() {
    }

    public static Path get() {
        return ROOT;
    }

    public static Path get(String path) {
        return ROOT.resolve(path);
    }
}
