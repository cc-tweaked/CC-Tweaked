/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import dan200.computercraft.api.filesystem.IWritableMount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileMountTest {
    private static final long CAPACITY = 1_000_000;
    private final List<Path> cleanup = new ArrayList<>();

    @AfterEach
    public void cleanup() throws IOException {
        for (var mount : cleanup) MoreFiles.deleteRecursively(mount, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    private Path createRoot() throws IOException {
        var path = Files.createTempDirectory("cctweaked-test");
        cleanup.add(path);
        return path;
    }

    private IWritableMount getExisting(long capacity) throws IOException {
        return new FileMount(createRoot().toFile(), capacity);
    }

    private IWritableMount getNotExisting(long capacity) throws IOException {
        return new FileMount(createRoot().resolve("mount").toFile(), capacity);
    }

    @Test
    public void testRootWritable() throws IOException {
        assertFalse(getExisting(CAPACITY).isReadOnly("/"));
        assertFalse(getNotExisting(CAPACITY).isReadOnly("/"));
    }

    @Test
    public void testMissingDirWritable() throws IOException {
        assertFalse(getExisting(CAPACITY).isReadOnly("/foo/bar/baz/qux"));
    }

    @Test
    public void testDirReadOnly() throws IOException {
        var root = createRoot();
        var mount = new FileMount(root.toFile(), CAPACITY);
        mount.makeDirectory("read-only");

        var attributes = Files.getFileAttributeView(root.resolve("read-only"), PosixFileAttributeView.class);
        Assumptions.assumeTrue(attributes != null, "POSIX attributes are not available.");

        assertFalse(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        attributes.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
        assertTrue(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        assertTrue(mount.isReadOnly("read-only/child"), "Child should be read-only");
    }
}
