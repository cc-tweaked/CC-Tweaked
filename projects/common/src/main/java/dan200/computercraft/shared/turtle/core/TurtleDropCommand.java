// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.platform.ContainerTransfer;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LevelEvent;

public class TurtleDropCommand implements TurtleCommand {
    private final InteractDirection direction;
    private final int quantity;

    public TurtleDropCommand(InteractDirection direction, int quantity) {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Dropping nothing is easy
        if (quantity == 0) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        var source = TurtleUtil.getSelectedSlot(turtle);

        // Get inventory for thing in front
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);
        var side = direction.getOpposite();

        var inventory = PlatformHelper.get().getContainer((ServerLevel) world, newPosition, side);

        int transferred;
        if (inventory != null) {
            transferred = source.moveTo(inventory, quantity);
        } else {
            var stack = turtle.getInventory().removeItem(turtle.getSelectedSlot(), quantity);
            if (stack.isEmpty()) {
                transferred = ContainerTransfer.NO_ITEMS;
            } else {
                // Drop the item into the world
                turtle.getInventory().setChanged();
                transferred = stack.getCount();

                WorldUtil.dropItemStack(world, oldPosition, direction, stack);
                world.globalLevelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, newPosition, 0);
            }
        }

        switch (transferred) {
            case ContainerTransfer.NO_SPACE:
                return TurtleCommandResult.failure("No space for items");
            case ContainerTransfer.NO_ITEMS:
                return TurtleCommandResult.failure("No items to drop");
            default:
                turtle.playAnimation(TurtleAnimation.WAIT);
                return TurtleCommandResult.success();
        }
    }
}
