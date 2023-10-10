// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.turtle.upgrades.TurtleInventoryCrafting;

public class TurtleCraftCommand implements TurtleCommand {
    private final int limit;

    public TurtleCraftCommand(int limit) {
        this.limit = limit;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Craft the item
        var crafting = new TurtleInventoryCrafting(turtle);
        var results = crafting.doCrafting(turtle.getLevel(), limit);
        if (results == null) return TurtleCommandResult.failure("No matching recipes");

        // Store or drop any remainders
        for (var stack : results) TurtleUtil.storeItemOrDrop(turtle, stack);

        if (!results.isEmpty()) turtle.playAnimation(TurtleAnimation.WAIT);
        return TurtleCommandResult.success();
    }
}
