/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

/**
 * A function called when a turtle attempts to refuel via {@code turtle.refuel()}. This may be used to provide
 * alternative fuel sources, such as consuming RF batteries.
 *
 * @see ComputerCraftAPI#registerRefuelHandler(TurtleRefuelHandler)
 */
public interface TurtleRefuelHandler
{
    /**
     * Refuel a turtle using an item.
     *
     * @param turtle The turtle to refuel.
     * @param stack  The stack to refuel with.
     * @param slot   The slot the stack resides within. This may be used to modify the inventory afterwards.
     * @param limit  The maximum number of refuel operations to perform. This will often correspond to the number of
     *               items to consume.
     *               <p>
     *               This value may be zero. In this case, you should still detect if the item can be handled (returning
     *               {@code OptionalInt#of(0)} if so), but should <em>NOT</em> modify the stack or inventory.
     * @return The amount of fuel gained, or {@link OptionalInt#empty()} if this handler does not accept the given item.
     */
    OptionalInt refuel( ITurtleAccess turtle, ItemStack stack, int slot, int limit );
}
