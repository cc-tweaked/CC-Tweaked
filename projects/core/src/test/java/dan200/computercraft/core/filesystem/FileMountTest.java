// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.test.core.filesystem.MountContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileMountTest implements MountContract {
    private final List<Path> cleanup = new ArrayList<>();

    @Override
    public Mount createSkeleton() throws IOException {
        var path = Files.createTempDirectory("cctweaked-test");
        cleanup.add(path);

        Files.createDirectories(path.resolve("dir"));
        try (var writer = Files.newBufferedWriter(path.resolve("dir/file.lua"))) {
            writer.write("print('testing')");
        }
        Files.setLastModifiedTime(path.resolve("dir/file.lua"), MODIFY_TIME);
        Files.newBufferedWriter(path.resolve("f.lua")).close();

        return new FileMount(path);
    }

    private Mount createEmpty() throws IOException {
        var path = Files.createTempDirectory("cctweaked-test");
        cleanup.add(path);
        return new FileMount(path.resolve("empty"));
    }

    @AfterEach
    public void cleanup() throws IOException {
        for (var mount : cleanup) MoreFiles.deleteRecursively(mount, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    @Test
    public void testRootExistsWhenEmpty() throws IOException {
        var mount = createEmpty();
        assertTrue(mount.exists(""), "Root always exists");
        assertTrue(mount.isDirectory(""), "Root always is a directory");
    }

    @Test
    public void testListWhenEmpty() throws IOException {
        var mount = createEmpty();

        List<String> list = new ArrayList<>();
        mount.list("", list);
        assertEquals(List.of(), list, "Root has no children");

        assertThrows(IOException.class, () -> mount.list("elsewhere", list));
    }

    @Test
    public void testAttributesWhenEmpty() throws IOException {
        var mount = createEmpty();

        assertEquals(0, mount.getSize(""));
        assertNotNull(mount.getAttributes(""));
    }
}
