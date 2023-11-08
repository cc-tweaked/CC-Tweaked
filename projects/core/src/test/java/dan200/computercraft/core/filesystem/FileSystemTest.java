// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import com.google.common.io.Files;
import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.core.TestFiles;
import dan200.computercraft.core.apis.handles.WriteHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileSystemTest {
    private static final File ROOT = TestFiles.get("filesystem").toFile();
    private static final long CAPACITY = 1000000;

    private static FileSystem mkFs() throws FileSystemException {
        WritableMount writableMount = new WritableFileMount(ROOT, CAPACITY);
        return new FileSystem("hdd", writableMount);

    }

    /**
     * Ensures writing a file truncates it.
     *
     * @throws FileSystemException When the file system cannot be constructed.
     * @throws LuaException        When Lua functions fail.
     * @throws IOException         When reading and writing from strings
     */
    @Test
    public void testWriteTruncates() throws FileSystemException, LuaException, IOException {
        var fs = mkFs();

        {
            var writer = fs.openForWrite("out.txt", MountConstants.WRITE_OPTIONS);
            var handle = WriteHandle.of(writer.get(), writer, false, true);
            handle.write(new ObjectArguments("This is a long line"));
            handle.close();
        }

        assertEquals("This is a long line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());

        {
            var writer = fs.openForWrite("out.txt", MountConstants.WRITE_OPTIONS);
            var handle = WriteHandle.of(writer.get(), writer, false, true);
            handle.write(new ObjectArguments("Tiny line"));
            handle.close();
        }

        assertEquals("Tiny line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());
    }

    @Test
    public void testUnmountCloses() throws FileSystemException {
        var fs = mkFs();
        WritableMount mount = new WritableFileMount(new File(ROOT, "child"), CAPACITY);
        fs.mountWritable("disk", "disk", mount);

        var writer = fs.openForWrite("disk/out.txt", MountConstants.WRITE_OPTIONS);
        var handle = WriteHandle.of(writer.get(), writer, false, true);

        fs.unmount("disk");

        var err = assertThrows(LuaException.class, () -> handle.write(new ObjectArguments("Tiny line")));
        assertEquals("attempt to use a closed file", err.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sanitiseCases")
    public void testSanitize(String input, String output) {
        assertEquals(output, FileSystem.sanitizePath(input, false));
    }

    public static String[][] sanitiseCases() {
        return new String[][]{
            new String[]{ "a//b", "a/b" },
            new String[]{ "a/./b", "a/b" },
            new String[]{ "a/../b", "b" },
            new String[]{ "a/.../b", "a/b" },
            new String[]{ " a ", "a" },
            new String[]{ "a b c", "a b c" },
        };
    }
}
