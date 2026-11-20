// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.platform.ItemContainer;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LevelEvent;

public class TurtleDropCommand implements TurtleCommand {
    private final InteractDirection direction;
    private final int quantity;
    private final int toSlot;

    public TurtleDropCommand(InteractDirection direction, int quantity) {
        this(direction, quantity, -1);
    }

    /**
     * Construct a new drop command.
     *
     * @param direction The direction to drop in.
     * @param quantity  The maximum number of items to drop.
     * @param toSlot    The 0-indexed slot of the inventory to insert items into, or {@code -1} to insert into any slot.
     */
    public TurtleDropCommand(InteractDirection direction, int quantity, int toSlot) {
        this.direction = direction;
        this.quantity = quantity;
        this.toSlot = toSlot;
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

        // Get inventory for thing in front
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);
        var side = direction.getOpposite();

        var inventory = PlatformHelper.get().getContainer((ServerLevel) world, newPosition, side);

        int transferred;
        if (inventory != null) {
            if (toSlot >= 0) {
                if (toSlot >= inventory.size()) return TurtleCommandResult.failure("Slot out of range");
                inventory = inventory.singleSlot(toSlot);
                if (inventory == null) return TurtleCommandResult.failure("Cannot drop into a specific slot of this inventory");
            }

            transferred = TurtleUtil.getInventory(turtle).moveTo(turtle.getSelectedSlot(), inventory, quantity);
        } else if (toSlot >= 0) {
            return TurtleCommandResult.failure("No inventory to drop into");
        } else {
            var stack = turtle.getInventory().removeItem(turtle.getSelectedSlot(), quantity);
            if (stack.isEmpty()) {
                transferred = ItemContainer.NO_ITEMS;
            } else {
                // Drop the item into the world
                turtle.getInventory().setChanged();
                transferred = stack.getCount();

                WorldUtil.dropItemStack(world, oldPosition, direction, stack);
                world.globalLevelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, newPosition, 0);
            }
        }

        return switch (transferred) {
            case ItemContainer.NO_SPACE -> TurtleCommandResult.failure("No space for items");
            case ItemContainer.NO_ITEMS -> TurtleCommandResult.failure("No items to drop");
            default -> {
                turtle.playAnimation(TurtleAnimation.WAIT);
                yield TurtleCommandResult.success();
            }
        };
    }
}
