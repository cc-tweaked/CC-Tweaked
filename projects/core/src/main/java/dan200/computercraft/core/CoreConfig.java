// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core;

/**
 * Config options for ComputerCraft's Lua runtime.
 */
public final class CoreConfig {
    // TODO: Ideally this would be an instance in {@link ComputerContext}, but sharing this everywhere it needs to be is
    //  tricky.

    private CoreConfig() {
    }

    public static int maximumFilesOpen = 128;
    public static String defaultComputerSettings = "";

    public static boolean httpWebsocketEnabled = true;
}
