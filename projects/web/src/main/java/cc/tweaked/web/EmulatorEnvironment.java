// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import cc.tweaked.web.js.Callbacks;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.computer.GlobalEnvironment;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * The {@link GlobalEnvironment} for all {@linkplain EmulatedComputer emulated computers}. This reads resources and
 * version information from {@linkplain Callbacks the resources module}.
 */
final class EmulatorEnvironment implements GlobalEnvironment {
    public static final EmulatorEnvironment INSTANCE = new EmulatorEnvironment();

    private final String version = Callbacks.getModVersion();
    private @Nullable ResourceMount romMount;
    private byte @Nullable [] bios;

    private EmulatorEnvironment() {
    }

    @Override
    public String getHostString() {
        return "ComputerCraft " + version + " (tweaked.cc)";
    }

    @Override
    public String getUserAgent() {
        return "computercraft/" + version;
    }

    @Override
    public Mount getRomMount() {
        return romMount != null ? romMount : (romMount = new ResourceMount());
    }

    @Override
    public InputStream getLuaBios() {
        var biosContents = bios != null ? bios : (bios = Callbacks.getResource("bios.lua"));
        return new ByteArrayInputStream(biosContents);
    }
}
