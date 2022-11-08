/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/**
 * A (possibly cached) provider of a component at a specific location.
 *
 * @param <T> The type of the component.
 */
public interface ComponentAccess<T> {
    /**
     * Get a peripheral for the current block.
     * <p>
     * Both {@code level} and {@code pos} must be constant for the lifetime of the store.
     *
     * @param level     The current level.
     * @param pos       The position of the block fetching the peripheral, for instance the computer or modem.
     * @param direction The direction the peripheral is in.
     * @return The peripheral, or {@literal null} if not found.
     * @throws IllegalStateException If the level or position have changed.
     */
    @Nullable
    T get(ServerLevel level, BlockPos pos, Direction direction);
}
