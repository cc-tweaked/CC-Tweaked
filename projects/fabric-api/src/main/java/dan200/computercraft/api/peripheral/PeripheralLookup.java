/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * {@linkplain BlockApiLookup Block API lookup} for {@link IPeripheral}s. This should be used to register peripherals
 * for a block. It should <em>NOT</em> be used to query peripherals.
 */
public final class PeripheralLookup {
    public static final ResourceLocation ID = new ResourceLocation(ComputerCraftAPI.MOD_ID, "peripheral");

    private static final BlockApiLookup<IPeripheral, Direction> lookup = BlockApiLookup.get(ID, IPeripheral.class, Direction.class);

    private PeripheralLookup() {
    }

    public static BlockApiLookup<IPeripheral, Direction> get() {
        return lookup;
    }
}
