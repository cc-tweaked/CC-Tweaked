// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The contract that all {@link Mount} implementations must fulfill.
 *
 * @see WritableMountContract
 */
public interface MountContract {
    FileTime EPOCH = FileTime.from(Instant.EPOCH);
    FileTime MODIFY_TIME = FileTime.from(Instant.EPOCH.plus(2, ChronoUnit.DAYS));

    /**
     * Create a skeleton mount. This should contain the following files:
     *
     * <ul>
     *     <li>{@code dir/file.lua}, containing {@code print('testing')}.</li>
     *     <li>{@code f.lua}, containing nothing.</li>
     * </ul>
     *
     * @return The skeleton mount.
     */
    Mount createSkeleton() throws IOException;

    /**
     * Determine if this attributes provided by this mount support file times.
     *
     * @return Whether this mount supports {@link BasicFileAttributes#lastModifiedTime()}.
     */
    default boolean hasFileTimes() {
        return true;
    }

    @Test
    default void testIsDirectory() throws IOException {
        var mount = createSkeleton();

        assertTrue(mount.isDirectory(""), "Root should be directory");
        assertTrue(mount.isDirectory("dir"), "dir/ should be directory");
        assertFalse(mount.isDirectory("dir/file.lua"), "dir/file.lua should not be a directory");
        assertFalse(mount.isDirectory("doesnt/exist"), "doesnt/exist should not be a directory");
    }

    @Test
    default void testExists() throws IOException {
        var mount = createSkeleton();

        assertTrue(mount.exists(""), "Root should exist");
        assertTrue(mount.exists("dir"), "dir/ should exist");
        assertFalse(mount.isDirectory("doesnt/exist"), "doesnt/exist should not exist");
    }

    @Test
    default void testList() throws IOException {
        var mount = createSkeleton();

        List<String> list = new ArrayList<>();
        mount.list("", list);
        list.sort(Comparator.naturalOrder());

        assertEquals(List.of("dir", "f.lua"), list);
    }

    @Test
    default void testOpenFile() throws IOException {
        var mount = createSkeleton();

        byte[] contents;
        try (var stream = mount.openForRead("dir/file.lua")) {
            contents = Channels.newInputStream(stream).readAllBytes();
        }

        assertEquals(new String(contents, StandardCharsets.UTF_8), "print('testing')");
    }

    @Test
    default void testOpenFileFails() throws IOException {
        var mount = createSkeleton();

        var exn = assertThrows(FileOperationException.class, () -> mount.openForRead("doesnt/exist"));
        assertEquals("doesnt/exist", exn.getFilename());
        assertEquals("No such file", exn.getMessage());
    }

    @Test
    default void testSize() throws IOException {
        var mount = createSkeleton();

        assertEquals(0, mount.getSize("f.lua"), "Empty file has 0 size");
        assertEquals("print('testing')".length(), mount.getSize("dir/file.lua"));
        assertEquals(0, mount.getSize("dir"), "Directory has 0 size");
    }

    @Test
    default void testFileAttributes() throws IOException {
        var attributes = createSkeleton().getAttributes("dir/file.lua");
        assertFalse(attributes.isDirectory());
        assertEquals("print('testing')".length(), attributes.size());
        assertEquals(hasFileTimes() ? MODIFY_TIME : EPOCH, attributes.lastModifiedTime());
    }

    @Test
    default void testDirectoryAttributes() throws IOException {
        var attributes = createSkeleton().getAttributes("");
        assertTrue(attributes.isDirectory());
    }
}
