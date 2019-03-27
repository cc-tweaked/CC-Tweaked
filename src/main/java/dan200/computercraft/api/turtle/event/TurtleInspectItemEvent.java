/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.api.turtle.event;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * Fired when a turtle gathers data on an item in its inventory.
 *
 * You may prevent items being inspected, or add additional information to the result. Be aware that this is fired on
 * the computer thread, and so any operations on it must be thread safe.
 *
 * @see TurtleAction#INSPECT_ITEM
 */
public class TurtleInspectItemEvent extends TurtleActionEvent
{
    private final ItemStack stack;
    private final Map<String, Object> data;

    public TurtleInspectItemEvent( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack stack, @Nonnull Map<String, Object> data )
    {
        super( turtle, TurtleAction.INSPECT_ITEM );

        Objects.requireNonNull( stack, "stack cannot be null" );
        Objects.requireNonNull( data, "data cannot be null" );
        this.stack = stack;
        this.data = data;
    }

    /**
     * The item which is currently being inspected.
     *
     * @return The item stack which is being inspected. This should <b>not</b> be modified.
     */
    @Nonnull
    public ItemStack getStack()
    {
        return stack;
    }

    /**
     * Get the "inspection data" from this item, which will be returned to the user.
     *
     * @return This items's inspection data.
     */
    @Nonnull
    public Map<String, Object> getData()
    {
        return data;
    }

    /**
     * Add new information to the inspection result. Note this will override fields with the same name.
     *
     * @param newData The data to add. Note all values should be convertible to Lua (see
     *                {@link dan200.computercraft.api.peripheral.IPeripheral#callMethod(IComputerAccess, ILuaContext, int, Object[])}).
     */
    public void addData( @Nonnull Map<String, ?> newData )
    {
        Objects.requireNonNull( newData, "newData cannot be null" );
        data.putAll( newData );
    }
}
