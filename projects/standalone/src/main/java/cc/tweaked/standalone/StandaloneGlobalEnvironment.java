// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.filesystem.FileMount;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@link GlobalEnvironment} for our standalone emulator.
 */
public class StandaloneGlobalEnvironment implements GlobalEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneGlobalEnvironment.class);
    private final Path resourceRoot;

    public StandaloneGlobalEnvironment(Path resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    @Override
    public String getHostString() {
        return "ComputerCraft (standalone)";
    }

    @Override
    public String getUserAgent() {
        return "computercraft/1.0";
    }

    @Override
    public Mount getRomMount() {
        return new FileMount(resourceRoot.resolve("data/computercraft/lua/rom"));
    }

    @Override
    public @Nullable InputStream getLuaBios() {
        var path = resourceRoot.resolve("data/computercraft/lua/bios.lua");
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            LOG.error("Failed to create resource file from {}.", path);
            return null;
        }
    }
}
