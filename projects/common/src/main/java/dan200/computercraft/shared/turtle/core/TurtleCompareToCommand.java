/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import net.minecraft.world.item.ItemStack;

public class TurtleCompareToCommand implements TurtleCommand {
    private final int slot;

    public TurtleCompareToCommand(int slot) {
        this.slot = slot;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        var selectedStack = turtle.getInventory().getItem(turtle.getSelectedSlot());
        var stack = turtle.getInventory().getItem(slot);
        return ItemStack.isSameItemSameTags(selectedStack, stack)
            ? TurtleCommandResult.success()
            : TurtleCommandResult.failure();
    }
}
