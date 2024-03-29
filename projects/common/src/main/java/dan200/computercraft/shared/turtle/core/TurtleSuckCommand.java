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
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;

public class TurtleSuckCommand implements TurtleCommand {
    private final InteractDirection direction;
    private final int quantity;

    public TurtleSuckCommand(InteractDirection direction, int quantity) {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Sucking nothing is easy
        if (quantity == 0) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Get inventory for thing in front
        var world = turtle.getLevel();
        var turtlePosition = turtle.getPosition();
        var blockPosition = turtlePosition.relative(direction);
        var side = direction.getOpposite();

        var inventory = PlatformHelper.get().getContainer((ServerLevel) world, blockPosition, side);

        if (inventory != null) {
            // Take from inventory of thing in front
            var transferred = inventory.moveTo(TurtleUtil.getOffsetInventory(turtle), quantity);
            switch (transferred) {
                case ContainerTransfer.NO_SPACE:
                    return TurtleCommandResult.failure("No space for items");
                case ContainerTransfer.NO_ITEMS:
                    return TurtleCommandResult.failure("No items to take");
                default:
                    turtle.playAnimation(TurtleAnimation.WAIT);
                    return TurtleCommandResult.success();
            }
        } else {
            // Suck up loose items off the ground
            var aabb = new AABB(
                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(),
                blockPosition.getX() + 1.0, blockPosition.getY() + 1.0, blockPosition.getZ() + 1.0
            );
            var list = world.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
            if (list.isEmpty()) return TurtleCommandResult.failure("No items to take");

            for (var entity : list) {
                // Suck up the item
                var stack = entity.getItem().copy();

                ItemStack storeStack;
                ItemStack leaveStack;
                if (stack.getCount() > quantity) {
                    storeStack = stack.split(quantity);
                    leaveStack = stack;
                } else {
                    storeStack = stack;
                    leaveStack = ItemStack.EMPTY;
                }

                var oldCount = storeStack.getCount();
                var remainder = InventoryUtil.storeItemsFromOffset(turtle.getInventory(), storeStack, turtle.getSelectedSlot());

                if (remainder.getCount() != oldCount) {
                    if (remainder.isEmpty() && leaveStack.isEmpty()) {
                        entity.discard();
                    } else if (remainder.isEmpty()) {
                        entity.setItem(leaveStack);
                    } else if (leaveStack.isEmpty()) {
                        entity.setItem(remainder);
                    } else {
                        leaveStack.grow(remainder.getCount());
                        entity.setItem(leaveStack);
                    }

                    // Play fx
                    world.globalLevelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, turtlePosition, 0);
                    turtle.playAnimation(TurtleAnimation.WAIT);
                    return TurtleCommandResult.success();
                }
            }


            return TurtleCommandResult.failure("No space for items");
        }
    }
}
