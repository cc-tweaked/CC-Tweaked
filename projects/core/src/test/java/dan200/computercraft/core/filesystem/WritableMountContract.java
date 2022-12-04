/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.WritableMount;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The contract that all {@link WritableMount}s must fulfill.
 */
public interface WritableMountContract {
    long CAPACITY = 1_000_000;

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
    default void testRootWritable() throws IOException {
        assertFalse(createExisting(CAPACITY).mount().isReadOnly("/"));
        assertFalse(createMount(CAPACITY).mount().isReadOnly("/"));
    }

    @Test
    default void testMissingDirWritable() throws IOException {
        assertFalse(createExisting(CAPACITY).mount().isReadOnly("/foo/bar/baz/qux"));
    }

    @Test
    default void testDirReadOnly() throws IOException {
        var root = createMount(CAPACITY);
        var mount = root.mount();
        mount.makeDirectory("read-only");

        assertFalse(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        root.makeReadOnly("read-only");
        assertTrue(mount.isReadOnly("read-only"), "Directory should not be read-only yet");
        assertTrue(mount.isReadOnly("read-only/child"), "Child should be read-only");
    }

    @Test
    default void testMovePreservesSpace() throws IOException {
        var access = createExisting(CAPACITY);
        var mount = access.mount();
        mount.openForWrite("foo").close();

        var remainingSpace = mount.getRemainingSpace();
        mount.rename("foo", "bar");

        assertEquals(remainingSpace, mount.getRemainingSpace(), "Free space has changed after moving");
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
         * Make a path read-only. This may throw a {@link TestAbortedException} if
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
