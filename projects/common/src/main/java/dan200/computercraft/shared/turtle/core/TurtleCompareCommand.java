// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TurtleCompareCommand implements TurtleCommand {
    private final InteractDirection direction;

    public TurtleCompareCommand(InteractDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Get currently selected stack
        var selectedStack = turtle.getInventory().getItem(turtle.getSelectedSlot());

        // Get stack representing thing in front
        var world = turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);

        var lookAtStack = ItemStack.EMPTY;
        if (!world.isEmptyBlock(newPosition)) {
            var lookAtState = world.getBlockState(newPosition);
            var lookAtBlock = lookAtState.getBlock();
            if (!lookAtState.isAir()) {
                // See if the block drops anything with the same ID as itself
                // (try 5 times to try and beat random number generators)
                for (var i = 0; i < 5 && lookAtStack.isEmpty(); i++) {
                    var drops = Block.getDrops(lookAtState, (ServerLevel) world, newPosition, world.getBlockEntity(newPosition));
                    if (!drops.isEmpty()) {
                        for (var drop : drops) {
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
        return selectedStack.getItem() == lookAtStack.getItem()
            ? TurtleCommandResult.success()
            : TurtleCommandResult.failure();
    }
}
