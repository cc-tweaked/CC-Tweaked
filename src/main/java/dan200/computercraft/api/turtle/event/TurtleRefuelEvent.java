/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle.event;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Fired when a turtle attempts to refuel from an item.
 *
 * One may use {@link #setHandler(Handler)} to register a custom fuel provider for a given item.
 */
public class TurtleRefuelEvent extends TurtleEvent
{
    private final ItemStack stack;
    private Handler handler;

    public TurtleRefuelEvent( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack stack )
    {
        super( turtle );

        Objects.requireNonNull( turtle, "turtle cannot be null" );
        this.stack = stack;
    }

    /**
     * Get the stack we are attempting to refuel from.
     *
     * Do not modify the returned stack - all modifications should be done within the {@link Handler}.
     *
     * @return The stack to refuel from.
     */
    public ItemStack getStack()
    {
        return stack;
    }

    /**
     * Get the refuel handler for this stack.
     *
     * @return The refuel handler, or {@code null} if none has currently been set.
     * @see #setHandler(Handler)
     */
    @Nullable
    public Handler getHandler()
    {
        return handler;
    }

    /**
     * Set the refuel handler for this stack.
     *
     * You should call this if you can actually refuel from this item, and ideally only if there are no existing
     * handlers.
     *
     * @param handler The new refuel handler.
     * @see #getHandler()
     */
    public void setHandler( @Nullable Handler handler )
    {
        this.handler = handler;
    }

    /**
     * Handles refuelling a turtle from a specific item.
     */
    @FunctionalInterface
    public interface Handler
    {
        /**
         * Refuel a turtle using an item.
         *
         * @param turtle The turtle to refuel.
         * @param stack  The stack to refuel with.
         * @param slot   The slot the stack resides within. This may be used to modify the inventory afterwards.
         * @param limit  The maximum number of refuel operations to perform. This will often correspond to the number of
         *               items to consume.
         * @return The amount of fuel gained.
         */
        int refuel( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack stack, int slot, int limit );
    }
}
