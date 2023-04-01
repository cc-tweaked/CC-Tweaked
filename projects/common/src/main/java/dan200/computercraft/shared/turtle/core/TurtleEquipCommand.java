// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.*;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.turtle.TurtleUtil;

public class TurtleEquipCommand implements TurtleCommand {
    private final TurtleSide side;

    public TurtleEquipCommand(TurtleSide side) {
        this.side = side;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Determine the upgrade to replace
        var oldUpgrade = turtle.getUpgrade(side);

        // Determine the upgrade to equipLeft
        ITurtleUpgrade newUpgrade;
        var selectedStack = turtle.getInventory().getItem(turtle.getSelectedSlot());
        if (!selectedStack.isEmpty()) {
            newUpgrade = TurtleUpgrades.instance().get(selectedStack);
            if (newUpgrade == null) return TurtleCommandResult.failure("Not a valid upgrade");
        } else {
            newUpgrade = null;
        }

        // Do the swapping:
        if (newUpgrade != null) turtle.getInventory().removeItem(turtle.getSelectedSlot(), 1);
        if (oldUpgrade != null) TurtleUtil.storeItemOrDrop(turtle, oldUpgrade.getCraftingItem().copy());
        turtle.setUpgrade(side, newUpgrade);

        // Animate
        if (newUpgrade != null || oldUpgrade != null) {
            turtle.playAnimation(TurtleAnimation.WAIT);
        }

        return TurtleCommandResult.success();
    }
}
