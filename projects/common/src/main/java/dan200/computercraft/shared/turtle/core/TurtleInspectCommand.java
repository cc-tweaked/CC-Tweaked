// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.detail.BlockReference;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public class TurtleInspectCommand implements TurtleCommand {
    private final InteractDirection direction;

    public TurtleInspectCommand(InteractDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Check if thing in front is air or not
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);

        var block = new BlockReference(world, newPosition);
        if (block.state().isAir()) return TurtleCommandResult.failure("No block to inspect");

        var table = VanillaDetailRegistries.BLOCK_IN_WORLD.getDetails(block);

        return TurtleCommandResult.success(new Object[]{ table });

    }
}
