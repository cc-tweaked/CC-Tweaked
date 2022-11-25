/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.node.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * {@linkplain BlockApiLookup Block API lookup} for {@link WiredElement}s. This should be used to query wired elements
 * from a block.
 */
public final class WiredElementLookup {
    public static final ResourceLocation ID = new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_node");

    private static final BlockApiLookup<WiredElement, Direction> lookup = BlockApiLookup.get(ID, WiredElement.class, Direction.class);

    private WiredElementLookup() {
    }

    public static BlockApiLookup<WiredElement, Direction> get() {
        return lookup;
    }
}
