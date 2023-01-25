/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.world.item.ItemStack;

/**
 * An internal version of {@link ITurtleAccess}.
 * <p>
 * This exposes additional functionality we don't want in the public API, but where we don't want access to the full
 * {@link TurtleBrain} interface.
 */
public interface TurtleAccessInternal extends ITurtleAccess {
    /**
     * Get an immutable snapshot of an item in the inventory. This is a thread-safe version of
     * {@code getInventory().getItem()}.
     *
     * @param slot The slot
     * @return The current item. This should NOT be modified.
     * @see net.minecraft.world.Container#getItem(int)
     */
    ItemStack getItemSnapshot(int slot);
}
