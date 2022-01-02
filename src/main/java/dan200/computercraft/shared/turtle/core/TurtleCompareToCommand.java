/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TurtleCompareToCommand implements ITurtleCommand
{
    private final int slot;

    public TurtleCompareToCommand( int slot )
    {
        this.slot = slot;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        ItemStack selectedStack = turtle.getInventory().getItem( turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getItem( slot );
        return InventoryUtil.areItemsStackable( selectedStack, stack )
            ? TurtleCommandResult.success()
            : TurtleCommandResult.failure();
    }
}
