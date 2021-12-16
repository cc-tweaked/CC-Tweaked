/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link net.minecraft.world.level.block.entity.BlockEntity} which may act as a peripheral.
 *
 * If you need more complex capabilities (such as handling TEs not belonging to your mod), you should use {@link IPeripheralProvider}.
 */
public interface IPeripheralTile
{
    /**
     * Get the peripheral on the given {@code side}.
     *
     * @param side The side to get the peripheral from.
     * @return A peripheral, or {@code null} if there is not a peripheral here.
     * @see IPeripheralProvider#getPeripheral(Level, BlockPos, Direction)
     */
    @Nullable
    IPeripheral getPeripheral( @Nonnull Direction side );
}
