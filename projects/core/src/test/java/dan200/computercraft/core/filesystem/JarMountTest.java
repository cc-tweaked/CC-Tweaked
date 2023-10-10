// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import com.google.common.io.ByteStreams;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.TestFiles;
import dan200.computercraft.test.core.CloseScope;
import dan200.computercraft.test.core.filesystem.MountContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class JarMountTest implements MountContract {
    private static final File ZIP_FILE = TestFiles.get("jar-mount.zip").toFile();

    private final CloseScope toClose = new CloseScope();

    @BeforeAll
    public static void before() throws IOException {
        ZIP_FILE.getParentFile().mkdirs();

        try (var stream = new ZipOutputStream(new FileOutputStream(ZIP_FILE))) {
            stream.putNextEntry(new ZipEntry("root/"));
            stream.closeEntry();

            stream.putNextEntry(new ZipEntry("root/dir/"));
            stream.closeEntry();

            stream.putNextEntry(new ZipEntry("root/dir/file.lua").setLastModifiedTime(MODIFY_TIME));
            stream.write("print('testing')".getBytes(StandardCharsets.UTF_8));
            stream.closeEntry();

            stream.putNextEntry(new ZipEntry("root/f.lua"));
            stream.closeEntry();
        }
    }

    @AfterEach
    public void after() throws Exception {
        toClose.close();
    }

    @Override
    public Mount createSkeleton() throws IOException {
        return toClose.add(new JarMount(ZIP_FILE, "root"));
    }

    private Mount createMount(String path) throws IOException {
        return toClose.add(new JarMount(ZIP_FILE, "root/" + path));
    }

    @Test
    public void mountsDir() throws IOException {
        var mount = createMount("dir");
        assertTrue(mount.isDirectory(""), "Root should be directory");
        assertTrue(mount.exists("file.lua"), "File should exist");
    }

    @Test
    public void mountsFile() throws IOException {
        var mount = createMount("dir/file.lua");
        assertTrue(mount.exists(""), "Root should exist");
        assertFalse(mount.isDirectory(""), "Root should be a file");
    }

    @Test
    public void opensFileFromFile() throws IOException {
        var mount = createMount("dir/file.lua");
        byte[] contents;
        try (var stream = mount.openForRead("")) {
            contents = ByteStreams.toByteArray(Channels.newInputStream(stream));
        }

        assertEquals(new String(contents, StandardCharsets.UTF_8), "print('testing')");
    }

    @Test
    public void opensFileFromDir() throws IOException {
        var mount = createMount("dir");
        byte[] contents;
        try (var stream = mount.openForRead("file.lua")) {
            contents = ByteStreams.toByteArray(Channels.newInputStream(stream));
        }

        assertEquals(new String(contents, StandardCharsets.UTF_8), "print('testing')");
    }
}
