// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import dan200.computercraft.api.component.ComputerComponent;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * An interface passed to {@link ILuaAPIFactory} in order to provide additional information
 * about a computer.
 */
@ApiStatus.NonExtendable
public interface IComputerSystem extends IComputerAccess {
    /**
     * Get the level this computer is currently in.
     * <p>
     * This method is not guaranteed to remain the same (even for stationary computers).
     *
     * @return The computer's current level.
     */
    ServerLevel getLevel();

    /**
     * Get the position this computer is currently at.
     * <p>
     * This method is not guaranteed to remain the same (even for stationary computers).
     *
     * @return The computer's current position.
     */
    BlockPos getPosition();

    /**
     * Get the label for this computer.
     *
     * @return This computer's label, or {@code null} if it is not set.
     */
    @Nullable
    String getLabel();

    /**
     * Get a component attached to this computer.
     * <p>
     * No component is guaranteed to be on a computer, and so this method should always be guarded with a null check.
     * <p>
     * This method will always return the same value for a given component, and so may be cached.
     *
     * @param component The component to query.
     * @param <T>       The type of the component.
     * @return The component, if present.
     */
    <T> @Nullable T getComponent(ComputerComponent<T> component);
}
