// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.shared.integration.libmultipart.LibMultiPartIntegration;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Constants indicating whether various mods are loaded or not. These are stored as static final fields, to avoid
 * repeated lookups and allow the JIT to inline them to constants.
 */
public final class LoadedMods {
    /**
     * Whether LibMultiPart is loaded.
     *
     * @see LibMultiPartIntegration
     */
    public static final boolean LIB_MULTI_PART = FabricLoader.getInstance().isModLoaded(LibMultiPartIntegration.MOD_ID);

    private LoadedMods() {
    }
}
