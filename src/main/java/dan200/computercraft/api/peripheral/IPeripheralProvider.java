/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface is used to create peripheral implementations for blocks.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)
 */
@FunctionalInterface
public interface IPeripheralProvider
{
    /**
     * Produce an peripheral implementation from a block location.
     *
     * @param world The world the block is in.
     * @param pos   The position the block is at.
     * @param side  The side to get the peripheral from.
     * @return A peripheral or {@literal null} if there is not a peripheral here you'd like to handle.
     * @see dan200.computercraft.api.ComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)
     */
    @Nullable
    IPeripheral getPeripheral( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side );
}
