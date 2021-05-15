/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.InventoryUtil;

import net.minecraft.item.ItemStack;

public class TurtleCompareToCommand implements ITurtleCommand {
    private final int m_slot;

    public TurtleCompareToCommand(int slot) {
        this.m_slot = slot;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        ItemStack selectedStack = turtle.getInventory()
                                        .getStack(turtle.getSelectedSlot());
        ItemStack stack = turtle.getInventory()
                                .getStack(this.m_slot);
        if (InventoryUtil.areItemsStackable(selectedStack, stack)) {
            return TurtleCommandResult.success();
        } else {
            return TurtleCommandResult.failure();
        }
    }
}
