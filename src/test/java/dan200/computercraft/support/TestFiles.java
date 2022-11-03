/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

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
