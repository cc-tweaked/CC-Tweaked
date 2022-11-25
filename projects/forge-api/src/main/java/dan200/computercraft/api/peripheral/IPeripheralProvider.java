/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;


/**
 * This interface is used to create peripheral implementations for blocks.
 * <p>
 * If you have a {@link BlockEntity} which acts as a peripheral, you may alternatively expose the {@link IPeripheral}
 * capability.
 * <p>
 * {@code dan200.computercraft.api.ForgeComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)} should be used
 * to register a peripheral provider.
 */
@FunctionalInterface
public interface IPeripheralProvider {
    // TODO(1.19.3): Move to Forge and fix link above.

    /**
     * Produce an peripheral implementation from a block location.
     *
     * @param world The world the block is in.
     * @param pos   The position the block is at.
     * @param side  The side to get the peripheral from.
     * @return A peripheral, or {@link LazyOptional#empty()} if there is not a peripheral here you'd like to handle.
     */
    LazyOptional<IPeripheral> getPeripheral(Level world, BlockPos pos, Direction side);
}
