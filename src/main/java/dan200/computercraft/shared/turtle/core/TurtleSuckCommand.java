/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import java.util.List;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.api.turtle.event.TurtleInventoryEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;

import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TurtleSuckCommand implements ITurtleCommand {
    private final InteractDirection m_direction;
    private final int m_quantity;

    public TurtleSuckCommand(InteractDirection direction, int quantity) {
        this.m_direction = direction;
        this.m_quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Sucking nothing is easy
        if (this.m_quantity == 0) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        Direction direction = this.m_direction.toWorldDir(turtle);

        // Get inventory for thing in front
        World world = turtle.getWorld();
        BlockPos turtlePosition = turtle.getPosition();
        BlockPos blockPosition = turtlePosition.offset(direction);
        Direction side = direction.getOpposite();

        Inventory inventory = InventoryUtil.getInventory(world, blockPosition, side);

        // Fire the event, exiting if it is cancelled.
        TurtlePlayer player = TurtlePlaceCommand.createPlayer(turtle, turtlePosition, direction);
        TurtleInventoryEvent.Suck event = new TurtleInventoryEvent.Suck(turtle, player, world, blockPosition, inventory);
        if (TurtleEvent.post(event)) {
            return TurtleCommandResult.failure(event.getFailureMessage());
        }

        if (inventory != null) {
            // Take from inventory of thing in front
            ItemStack stack = InventoryUtil.takeItems(this.m_quantity, ItemStorage.wrap(inventory));
            if (stack.isEmpty()) {
                return TurtleCommandResult.failure("No items to take");
            }

            // Try to place into the turtle
            ItemStack remainder = InventoryUtil.storeItems(stack, turtle.getItemHandler(), turtle.getSelectedSlot());
            if (!remainder.isEmpty()) {
                // Put the remainder back in the inventory
                InventoryUtil.storeItems(remainder, ItemStorage.wrap(inventory));
            }

            // Return true if we consumed anything
            if (remainder != stack) {
                turtle.playAnimation(TurtleAnimation.WAIT);
                return TurtleCommandResult.success();
            } else {
                return TurtleCommandResult.failure("No space for items");
            }
        } else {
            // Suck up loose items off the ground
            Box aabb = new Box(blockPosition.getX(),
                               blockPosition.getY(),
                               blockPosition.getZ(),
                               blockPosition.getX() + 1.0,
                               blockPosition.getY() + 1.0,
                               blockPosition.getZ() + 1.0);
            List<ItemEntity> list = world.getEntitiesByClass(ItemEntity.class, aabb, EntityPredicates.VALID_ENTITY);
            if (list.isEmpty()) {
                return TurtleCommandResult.failure("No items to take");
            }

            for (ItemEntity entity : list) {
                // Suck up the item
                ItemStack stack = entity.getStack()
                                        .copy();

                ItemStack storeStack;
                ItemStack leaveStack;
                if (stack.getCount() > this.m_quantity) {
                    storeStack = stack.split(this.m_quantity);
                    leaveStack = stack;
                } else {
                    storeStack = stack;
                    leaveStack = ItemStack.EMPTY;
                }

                ItemStack remainder = InventoryUtil.storeItems(storeStack, turtle.getItemHandler(), turtle.getSelectedSlot());

                if (remainder != storeStack) {
                    if (remainder.isEmpty() && leaveStack.isEmpty()) {
                        entity.remove();
                    } else if (remainder.isEmpty()) {
                        entity.setStack(leaveStack);
                    } else if (leaveStack.isEmpty()) {
                        entity.setStack(remainder);
                    } else {
                        leaveStack.increment(remainder.getCount());
                        entity.setStack(leaveStack);
                    }

                    // Play fx
                    world.syncGlobalEvent(1000, turtlePosition, 0); // BLOCK_DISPENSER_DISPENSE
                    turtle.playAnimation(TurtleAnimation.WAIT);
                    return TurtleCommandResult.success();
                }
            }


            return TurtleCommandResult.failure("No space for items");
        }
    }
}
