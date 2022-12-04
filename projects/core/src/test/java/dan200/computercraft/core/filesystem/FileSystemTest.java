/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.Files;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.core.TestFiles;
import dan200.computercraft.core.apis.handles.EncodedWritableHandle;
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
        WritableMount writableMount = new FileMount(ROOT, CAPACITY);
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
            var writer = fs.openForWrite("out.txt", false, EncodedWritableHandle::openUtf8);
            var handle = new EncodedWritableHandle(writer.get(), writer);
            handle.write(new ObjectArguments("This is a long line"));
            handle.doClose();
        }

        assertEquals("This is a long line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());

        {
            var writer = fs.openForWrite("out.txt", false, EncodedWritableHandle::openUtf8);
            var handle = new EncodedWritableHandle(writer.get(), writer);
            handle.write(new ObjectArguments("Tiny line"));
            handle.doClose();
        }

        assertEquals("Tiny line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());
    }

    @Test
    public void testUnmountCloses() throws FileSystemException {
        var fs = mkFs();
        WritableMount mount = new FileMount(new File(ROOT, "child"), CAPACITY);
        fs.mountWritable("disk", "disk", mount);

        var writer = fs.openForWrite("disk/out.txt", false, EncodedWritableHandle::openUtf8);
        var handle = new EncodedWritableHandle(writer.get(), writer);

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
