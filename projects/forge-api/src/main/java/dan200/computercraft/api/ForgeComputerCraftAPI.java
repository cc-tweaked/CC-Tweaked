// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.impl.ComputerCraftAPIForgeService;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * The forge-specific entrypoint for ComputerCraft's API.
 */
public final class ForgeComputerCraftAPI {
    private ForgeComputerCraftAPI() {
    }

    /**
     * Registers a capability that can be used by generic peripherals.
     *
     * @param capability The capability to register.
     * @see GenericSource
     */
    public static void registerGenericCapability(BlockCapability<?, Direction> capability) {
        getInstance().registerGenericCapability(capability);
    }

    private static ComputerCraftAPIForgeService getInstance() {
        return ComputerCraftAPIForgeService.get();
    }
}
