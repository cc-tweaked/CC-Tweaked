/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import java.util.List;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TurtleCompareCommand implements ITurtleCommand {
    private final InteractDirection m_direction;

    public TurtleCompareCommand(InteractDirection direction) {
        this.m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Get world direction from direction
        Direction direction = this.m_direction.toWorldDir(turtle);

        // Get currently selected stack
        ItemStack selectedStack = turtle.getInventory()
                                        .getStack(turtle.getSelectedSlot());

        // Get stack representing thing in front
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset(direction);

        ItemStack lookAtStack = ItemStack.EMPTY;
        if (!world.isAir(newPosition)) {
            BlockState lookAtState = world.getBlockState(newPosition);
            Block lookAtBlock = lookAtState.getBlock();
            if (!lookAtState.isAir()) {
                // See if the block drops anything with the same ID as itself
                // (try 5 times to try and beat random number generators)
                for (int i = 0; i < 5 && lookAtStack.isEmpty(); i++) {
                    List<ItemStack> drops = Block.getDroppedStacks(lookAtState, (ServerWorld) world, newPosition, world.getBlockEntity(newPosition));
                    if (!drops.isEmpty()) {
                        for (ItemStack drop : drops) {
                            if (drop.getItem() == lookAtBlock.asItem()) {
                                lookAtStack = drop;
                                break;
                            }
                        }
                    }
                }

                // Last resort: roll our own (which will probably be wrong)
                if (lookAtStack.isEmpty()) {
                    lookAtStack = new ItemStack(lookAtBlock);
                }
            }
        }

        // Compare them
        return selectedStack.getItem() == lookAtStack.getItem() ? TurtleCommandResult.success() : TurtleCommandResult.failure();
    }
}
