// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.test.core.ReplaceUnderscoresDisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.util.stream.Stream;

import static dan200.computercraft.api.filesystem.MountConstants.MINIMUM_FILE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The contract that all {@link WritableMount} implementations must fulfill.
 *
 * @see MountContract
 */
@DisplayNameGeneration(ReplaceUnderscoresDisplayNameGenerator.class)
public interface WritableMountContract {
    long CAPACITY = 1_000_000;

    String LONG_CONTENTS = "This is some example text.\n".repeat(100);

    static Stream<String> fileContents() {
        return Stream.of("", LONG_CONTENTS);
    }

    /**
     * Create a new empty mount.
     *
     * @param capacity The capacity of this mount
     * @return The newly created {@link WritableMount}.
     * @throws IOException If the mount could not be created.
     */
    MountAccess createMount(long capacity) throws IOException;

    /**
     * Create a new empty mount, ensuring it exists on disk.
     *
     * @param capacity The capacity of this mount
     * @return The newly created {@link WritableMount}.
     * @throws IOException If the mount could not be created.
     */
    default MountAccess createExisting(long capacity) throws IOException {
        var mount = createMount(capacity);
        mount.ensuresExist();
        return mount;
    }

    @Test
    default void Root_is_writable() throws IOException {
        assertFalse(createExisting(CAPACITY).mount().isReadOnly("/"));
        assertFalse(createMount(CAPACITY).mount().isReadOnly("/"));
    }

    @Test
    default void Missing_dir_is_writable() throws IOException {
        assertFalse(createExisting(CAPACITY).mount().isReadOnly("/foo/bar/baz/qux"));
    }

    @Test
    default void Make_directory_recursive() throws IOException {
        var access = createMount(CAPACITY);
        var mount = access.mount();
        mount.makeDirectory("a/b/c");

        assertTrue(mount.isDirectory("a/b/c"));

        assertEquals(CAPACITY - MINIMUM_FILE_SIZE * 3, mount.getRemainingSpace());
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    @Test
    default void Can_make_read_only() throws IOException {
        var root = createMount(CAPACITY);
        var mount = root.mount();
        mount.makeDirectory("read-only");

        assertFalse(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        root.makeReadOnly("read-only");
        assertTrue(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        assertTrue(mount.isReadOnly("read-only/child"), "Child should be read-only");
    }

    @Test
    default void Initial_free_space_and_capacity() throws IOException {
        var mount = createExisting(CAPACITY).mount();
        assertEquals(CAPACITY, mount.getCapacity());
        assertEquals(CAPACITY, mount.getRemainingSpace());
    }

    @Test
    default void Write_updates_size_and_free_space() throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();

        Mounts.writeFile(mount, "hello.txt", LONG_CONTENTS);
        assertEquals(LONG_CONTENTS.length(), mount.getSize("hello.txt"));
        assertEquals(CAPACITY - LONG_CONTENTS.length(), mount.getRemainingSpace());

        Mounts.writeFile(mount, "hello.txt", "");
        assertEquals(0, mount.getSize("hello.txt"));
        assertEquals(CAPACITY - MINIMUM_FILE_SIZE, mount.getRemainingSpace());
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    @Test
    default void Writing_uses_latest_file_size() throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();

        var handle = mount.openFile("file.txt", MountConstants.WRITE_OPTIONS);
        handle.write(LuaValues.encode(LONG_CONTENTS));
        assertEquals(CAPACITY - LONG_CONTENTS.length(), mount.getRemainingSpace());
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");

        var handle2 = mount.openFile("file.txt", MountConstants.WRITE_OPTIONS);

        handle.write(LuaValues.encode("test"));
        assertEquals(CAPACITY - LONG_CONTENTS.length() - 4, mount.getRemainingSpace());
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");

        handle2.close();
        handle.close();

        mount.delete("file.txt");
        assertEquals(CAPACITY, mount.getRemainingSpace());
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    @Test
    default void Append_jumps_to_file_end() throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();

        Mounts.writeFile(mount, "a.txt", "example");

        try (var handle = mount.openFile("a.txt", MountConstants.APPEND_OPTIONS)) {
            assertEquals(7, handle.position());
            handle.write(LuaValues.encode(" text"));
            assertEquals(12, handle.position());
        }

        assertEquals(12, mount.getSize("a.txt"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("fileContents")
    default void Move_file(String contents) throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();
        Mounts.writeFile(mount, "src.txt", contents);

        var remainingSpace = mount.getRemainingSpace();

        mount.rename("src.txt", "dest.txt");
        assertFalse(mount.exists("src.txt"));
        assertTrue(mount.exists("dest.txt"));

        assertEquals(contents.length(), mount.getSize("dest.txt"));
        assertEquals(remainingSpace, mount.getRemainingSpace(), "Free space has changed after moving");
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("fileContents")
    default void Move_file_fails_when_destination_exists(String contents) throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();
        Mounts.writeFile(mount, "src.txt", contents);
        Mounts.writeFile(mount, "dest.txt", "dest");

        var remainingSpace = mount.getRemainingSpace();

        assertThrows(IOException.class, () -> mount.rename("src.txt", "dest.txt"));

        assertEquals(contents.length(), mount.getSize("src.txt"));
        assertEquals(4, mount.getSize("dest.txt"));

        assertEquals(remainingSpace, mount.getRemainingSpace(), "Free space has changed despite no move occurred.");
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    @Test
    default void Move_file_fails_when_source_does_not_exist() throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();
        Mounts.writeFile(mount, "dest.txt", "dest");

        var remainingSpace = mount.getRemainingSpace();

        assertThrows(IOException.class, () -> mount.rename("src.txt", "dest.txt"));

        assertFalse(mount.exists("src.txt"));
        assertEquals(4, mount.getSize("dest.txt"));

        assertEquals(remainingSpace, mount.getRemainingSpace(), "Free space has changed despite no move occurred.");
        assertEquals(access.computeRemainingSpace(), access.mount().getRemainingSpace(), "Free space is inconsistent");
    }

    /**
     * Wraps a {@link WritableMount} with additional operations.
     */
    interface MountAccess {
        /**
         * Get the underlying mount.
         *
         * @return The actual mount.
         */
        WritableMount mount();

        /**
         * Make a path read-only. This may throw a {@link TestAbortedException} if this operation is not supported.
         *
         * @param path The mount-relative path.
         */
        void makeReadOnly(String path) throws IOException;

        /**
         * Ensures this mount exists.
         */
        void ensuresExist() throws IOException;

        /**
         * Get the remaining space for this mount.
         * <p>
         * This should recompute the value where possible, rather than using {@link WritableMount#getRemainingSpace()}:
         * its purpose is to ensure the value is accurate!
         *
         * @return The new
         * @throws IOException If the remaining space could not be computed.
         */
        long computeRemainingSpace() throws IOException;
    }
}
