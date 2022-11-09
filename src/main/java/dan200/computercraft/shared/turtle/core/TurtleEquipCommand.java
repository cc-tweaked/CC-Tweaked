/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.turtle.TurtleUtil;
import net.minecraft.world.item.ItemStack;

public class TurtleEquipCommand implements ITurtleCommand {
    private final TurtleSide side;

    public TurtleEquipCommand(TurtleSide side) {
        this.side = side;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Determine the upgrade to equipLeft
        ITurtleUpgrade newUpgrade;
        ItemStack newUpgradeStack;
        var selectedStack = turtle.getInventory().getItem(turtle.getSelectedSlot());
        if (!selectedStack.isEmpty()) {
            newUpgradeStack = selectedStack.copy();
            newUpgrade = TurtleUpgrades.instance().get(newUpgradeStack);
            if (newUpgrade == null) return TurtleCommandResult.failure("Not a valid upgrade");
        } else {
            newUpgradeStack = null;
            newUpgrade = null;
        }

        // Determine the upgrade to replace
        ItemStack oldUpgradeStack;
        var oldUpgrade = turtle.getUpgrade(side);
        if (oldUpgrade != null) {
            var craftingItem = oldUpgrade.getCraftingItem();
            oldUpgradeStack = !craftingItem.isEmpty() ? craftingItem.copy() : null;
        } else {
            oldUpgradeStack = null;
        }

        // Do the swapping:
        if (newUpgradeStack != null) turtle.getInventory().removeItem(turtle.getSelectedSlot(), 1);
        if (oldUpgradeStack != null) TurtleUtil.storeItemOrDrop(turtle, oldUpgradeStack);
        turtle.setUpgrade(side, newUpgrade);

        // Animate
        if (newUpgrade != null || oldUpgrade != null) {
            turtle.playAnimation(TurtleAnimation.WAIT);
        }

        return TurtleCommandResult.success();
    }
}
