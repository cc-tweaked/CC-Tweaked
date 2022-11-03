/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.Files;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import dan200.computercraft.core.apis.handles.EncodedWritableHandle;
import dan200.computercraft.support.TestFiles;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileSystemTest {
    private static final File ROOT = TestFiles.get("filesystem").toFile();
    private static final long CAPACITY = 1000000;

    private static FileSystem mkFs() throws FileSystemException {
        IWritableMount writableMount = new FileMount(ROOT, CAPACITY);
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
            var wrapper = new ObjectWrapper(new EncodedWritableHandle(writer.get(), writer));
            wrapper.call("write", "This is a long line");
            wrapper.call("close");
        }

        assertEquals("This is a long line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());

        {
            var writer = fs.openForWrite("out.txt", false, EncodedWritableHandle::openUtf8);
            var wrapper = new ObjectWrapper(new EncodedWritableHandle(writer.get(), writer));
            wrapper.call("write", "Tiny line");
            wrapper.call("close");
        }

        assertEquals("Tiny line", Files.asCharSource(new File(ROOT, "out.txt"), StandardCharsets.UTF_8).read());
    }

    @Test
    public void testUnmountCloses() throws FileSystemException {
        var fs = mkFs();
        IWritableMount mount = new FileMount(new File(ROOT, "child"), CAPACITY);
        fs.mountWritable("disk", "disk", mount);

        var writer = fs.openForWrite("disk/out.txt", false, EncodedWritableHandle::openUtf8);
        var wrapper = new ObjectWrapper(new EncodedWritableHandle(writer.get(), writer));

        fs.unmount("disk");

        var err = assertThrows(LuaException.class, () -> wrapper.call("write", "Tiny line"));
        assertEquals("attempt to use a closed file", err.getMessage());
    }
}
