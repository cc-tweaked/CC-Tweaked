// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * A {@linkplain BlockCapability block capability} for {@link WiredElement}s. This should be used to query wired elements
 * from a block.
 */
public final class WiredElementCapability {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "wired_node");

    private static final BlockCapability<WiredElement, Direction> capability = BlockCapability.create(ID, WiredElement.class, Direction.class);

    private WiredElementCapability() {
    }

    public static BlockCapability<WiredElement, Direction> get() {
        return capability;
    }
}
