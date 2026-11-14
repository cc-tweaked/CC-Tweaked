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
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class TurtleSuckCommand implements TurtleCommand {
    private final InteractDirection direction;
    private final int quantity;
    private final @Nullable Item item;
    private final int fromSlot;

    public TurtleSuckCommand(InteractDirection direction, int quantity) {
        this(direction, quantity, null, -1);
    }

    private TurtleSuckCommand(InteractDirection direction, int quantity, @Nullable Item item, int fromSlot) {
        this.direction = direction;
        this.quantity = quantity;
        this.item = item;
        this.fromSlot = fromSlot;
    }

    /**
     * Suck items from a specific slot of an inventory.
     *
     * @param direction The direction to suck from.
     * @param quantity  The maximum number of items to suck.
     * @param fromSlot  The 0-indexed slot of the inventory to take items from.
     * @return The suck command.
     */
    public static TurtleSuckCommand fromSlot(InteractDirection direction, int quantity, int fromSlot) {
        return new TurtleSuckCommand(direction, quantity, null, fromSlot);
    }

    /**
     * Suck a specific item, searching every slot of an inventory.
     *
     * @param direction The direction to suck from.
     * @param quantity  The maximum number of items to suck.
     * @param item      The item to take.
     * @return The suck command.
     */
    public static TurtleSuckCommand ofItem(InteractDirection direction, int quantity, Item item) {
        return new TurtleSuckCommand(direction, quantity, item, -1);
    }

    private boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (item == null || stack.is(item));
    }

    /**
     * Move up to {@link #quantity} matching items from an inventory, searching every slot.
     *
     * @param inventory   The inventory to take items from.
     * @param destination The inventory to move items to.
     * @return The number of items moved, or one of {@link ItemContainer#NO_ITEMS} or {@link ItemContainer#NO_SPACE}.
     */
    private int moveMatching(ItemContainer inventory, ItemContainer destination) {
        var moved = 0;
        var hasItems = false;
        for (int slot = 0, size = inventory.size(); slot < size && moved < quantity; slot++) {
            if (!matches(inventory.getItem(slot))) continue;
            hasItems = true;

            var transferred = inventory.moveTo(slot, destination, quantity - moved);
            if (transferred > 0) moved += transferred;
        }

        if (moved > 0) return moved;
        return hasItems ? ItemContainer.NO_SPACE : ItemContainer.NO_ITEMS;
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
            if (fromSlot >= inventory.size()) return TurtleCommandResult.failure("Slot out of range");

            // Take from inventory of thing in front
            var destination = TurtleUtil.getOffsetInventory(turtle);
            int transferred;
            if (fromSlot >= 0) {
                transferred = inventory.moveTo(fromSlot, destination, quantity);
            } else if (item != null) {
                transferred = moveMatching(inventory, destination);
            } else {
                transferred = inventory.moveTo(destination, quantity);
            }

            return switch (transferred) {
                case ItemContainer.NO_SPACE -> TurtleCommandResult.failure("No space for items");
                case ItemContainer.NO_ITEMS -> TurtleCommandResult.failure("No items to take");
                default -> {
                    turtle.playAnimation(TurtleAnimation.WAIT);
                    yield TurtleCommandResult.success();
                }
            };
        } else if (fromSlot >= 0) {
            return TurtleCommandResult.failure("No inventory to take from");
        } else {
            // Suck up loose items off the ground
            var aabb = new AABB(
                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(),
                blockPosition.getX() + 1.0, blockPosition.getY() + 1.0, blockPosition.getZ() + 1.0
            );
            var list = world.getEntitiesOfClass(
                ItemEntity.class, aabb, e -> EntitySelector.ENTITY_STILL_ALIVE.test(e) && matches(e.getItem())
            );
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
