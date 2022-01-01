/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * This interface is used to provide bundled redstone output for blocks.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerBundledRedstoneProvider(IBundledRedstoneProvider)
 */
@FunctionalInterface
public interface IBundledRedstoneProvider
{
    /**
     * Produce an bundled redstone output from a block location.
     *
     * @param world The world this block is in.
     * @param pos   The position this block is at.
     * @param side  The side to extract the bundled redstone output from.
     * @return A number in the range 0-65535 to indicate this block is providing output, or -1 if you do not wish to
     * handle this block.
     * @see dan200.computercraft.api.ComputerCraftAPI#registerBundledRedstoneProvider(IBundledRedstoneProvider)
     */
    int getBundledRedstoneOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side );
}
