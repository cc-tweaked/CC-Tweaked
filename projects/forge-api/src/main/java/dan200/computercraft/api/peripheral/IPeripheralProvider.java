// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.ForgeComputerCraftAPI;
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
 * {@link ForgeComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)} should be used to register a peripheral
 * provider.
 */
@FunctionalInterface
public interface IPeripheralProvider {
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
