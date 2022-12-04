/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import dan200.computercraft.api.filesystem.WritableMount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileMountTest implements WritableMountContract {
    private final List<Path> cleanup = new ArrayList<>();

    @AfterEach
    public void cleanup() throws IOException {
        for (var mount : cleanup) MoreFiles.deleteRecursively(mount, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    @Override
    public MountAccess createMount(long capacity) throws IOException {
        var path = Files.createTempDirectory("cctweaked-test");
        cleanup.add(path);
        return new MountAccessImpl(path.resolve("mount"), capacity);
    }

    private static final class MountAccessImpl implements MountAccess {
        private final Path root;
        private final long capacity;
        private final WritableMount mount;

        private MountAccessImpl(Path root, long capacity) {
            this.root = root;
            this.capacity = capacity;
            mount = new FileMount(root.toFile(), capacity);
        }

        @Override
        public WritableMount mount() {
            return mount;
        }

        @Override
        public void makeReadOnly(String path) {
            Assumptions.assumeTrue(root.resolve(path).toFile().setReadOnly(), "Change file to read-only");
        }

        @Override
        public void ensuresExist() throws IOException {
            Files.createDirectories(root);
        }

        @Override
        public long computeRemainingSpace() {
            return new FileMount(root.toFile(), capacity).getRemainingSpace();
        }
    }
}
