/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.google.common.base.Splitter;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class TurtlePlaceCommand implements TurtleCommand {
    private final InteractDirection direction;
    private final Object[] extraArguments;

    public TurtlePlaceCommand(InteractDirection direction, Object[] arguments) {
        this.direction = direction;
        extraArguments = arguments;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Get thing to place
        var stack = turtle.getInventory().getItem(turtle.getSelectedSlot());
        if (stack.isEmpty()) return TurtleCommandResult.failure("No items to place");

        // Remember old block
        var direction = this.direction.toWorldDir(turtle);

        // Create a fake player, and orient it appropriately
        var playerPosition = turtle.getPosition().relative(direction);
        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, playerPosition, direction);

        // Do the deploying
        turtlePlayer.loadInventory(turtle);
        var message = new ErrorMessage();
        var result = deploy(stack, turtle, turtlePlayer, direction, extraArguments, message);
        turtlePlayer.unloadInventory(turtle);
        if (result) {
            // Animate and return success
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        } else if (message.message != null) {
            return TurtleCommandResult.failure(message.message);
        } else {
            return TurtleCommandResult.failure(stack.getItem() instanceof BlockItem ? "Cannot place block here" : "Cannot place item here");
        }
    }

    public static boolean deployCopiedItem(
        ItemStack stack, ITurtleAccess turtle, Direction direction, @Nullable Object[] extraArguments, @Nullable ErrorMessage outErrorMessage
    ) {
        // Create a fake player, and orient it appropriately
        var playerPosition = turtle.getPosition().relative(direction);
        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, playerPosition, direction);
        turtlePlayer.loadInventory(stack);
        var result = deploy(stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage);
        turtlePlayer.player().getInventory().clearContent();
        return result;
    }

    private static boolean deploy(
        ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction,
        @Nullable Object[] extraArguments, @Nullable ErrorMessage outErrorMessage
    ) {
        // Deploy on an entity
        if (deployOnEntity(turtle, turtlePlayer)) return true;

        var position = turtle.getPosition();
        var newPosition = position.relative(direction);

        // Try to deploy against a block. Tries the following options:
        //     Deploy on the block immediately in front
        return deployOnBlock(stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, outErrorMessage)
            // Deploy on the block one block away
            || deployOnBlock(stack, turtle, turtlePlayer, newPosition.relative(direction), direction.getOpposite(), extraArguments, false, outErrorMessage)
            // Deploy down on the block in front
            || (direction.getAxis() != Direction.Axis.Y && deployOnBlock(stack, turtle, turtlePlayer, newPosition.below(), Direction.UP, extraArguments, false, outErrorMessage))
            // Deploy back onto the turtle
            || deployOnBlock(stack, turtle, turtlePlayer, position, direction, extraArguments, false, outErrorMessage);
    }

    private static boolean deployOnEntity(ITurtleAccess turtle, TurtlePlayer turtlePlayer) {
        // See if there is an entity present
        var world = turtle.getLevel();
        var turtlePos = turtlePlayer.player().position();
        var rayDir = turtlePlayer.player().getViewVector(1.0f);
        var hit = WorldUtil.clip(world, turtlePos, rayDir, 1.5, null);
        if (!(hit instanceof EntityHitResult entityHit)) return false;

        // Start claiming entity drops
        var hitEntity = entityHit.getEntity();
        var hitPos = entityHit.getLocation();

        DropConsumer.set(hitEntity, drop -> InventoryUtil.storeItemsFromOffset(turtlePlayer.player().getInventory(), drop, 1));
        var placed = PlatformHelper.get().interactWithEntity(turtlePlayer.player(), hitEntity, hitPos);
        TurtleUtil.stopConsuming(turtle);
        return placed;
    }

    private static boolean canDeployOnBlock(
        BlockPlaceContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position,
        Direction side, boolean allowReplaceable, @Nullable ErrorMessage outErrorMessage
    ) {
        var world = (ServerLevel) turtle.getLevel();
        if (!world.isInWorldBounds(position) || world.isEmptyBlock(position) ||
            (context.getItemInHand().getItem() instanceof BlockItem && WorldUtil.isLiquidBlock(world, position))) {
            return false;
        }

        var state = world.getBlockState(position);

        var replaceable = state.canBeReplaced(context);
        if (!allowReplaceable && replaceable) return false;

        // Check spawn protection
        var isProtected = replaceable
            ? player.isBlockProtected(world, position)
            : player.isBlockProtected(world, position.relative(side));
        if (isProtected) {
            if (outErrorMessage != null) outErrorMessage.message = "Cannot place in protected area";
            return false;
        }

        return true;
    }

    private static boolean deployOnBlock(
        ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side,
        @Nullable Object[] extraArguments, boolean allowReplace, @Nullable ErrorMessage outErrorMessage
    ) {
        // Re-orient the fake player
        var playerDir = side.getOpposite();
        var playerPosition = position.relative(side);
        turtlePlayer.setPosition(turtle, playerPosition, playerDir);

        // Calculate where the turtle would hit the block
        var hitX = 0.5f + side.getStepX() * 0.5f;
        var hitY = 0.5f + side.getStepY() * 0.5f;
        var hitZ = 0.5f + side.getStepZ() * 0.5f;
        if (Math.abs(hitY - 0.5f) < 0.01f) hitY = 0.45f;

        // Check if there's something suitable to place onto
        var hit = new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, position, false);
        var context = new UseOnContext(turtlePlayer.player(), InteractionHand.MAIN_HAND, hit);
        if (!canDeployOnBlock(new BlockPlaceContext(context), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage)) {
            return false;
        }

        var item = stack.getItem();
        var existingTile = turtle.getLevel().getBlockEntity(position);

        var placed = doDeployOnBlock(stack, turtlePlayer, hit).consumesAction();

        // Set text on signs
        if (placed && item instanceof SignItem && extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String message) {
            var world = turtle.getLevel();
            var tile = world.getBlockEntity(position);
            if (tile == null || tile == existingTile) {
                tile = world.getBlockEntity(position.relative(side));
            }

            if (tile instanceof SignBlockEntity) setSignText(world, tile, message);
        }

        return placed;
    }

    /**
     * Attempt to place an item into the world. Returns true/false if an item was placed.
     *
     * @param stack        The stack the player is using.
     * @param turtlePlayer The player which represents the turtle
     * @param hit          Where the block we're placing against was clicked.
     * @return If this item was deployed.
     */
    private static InteractionResult doDeployOnBlock(
        ItemStack stack, TurtlePlayer turtlePlayer, BlockHitResult hit
    ) {
        var result = PlatformHelper.get().useOn(turtlePlayer.player(), stack, hit);
        if (result != InteractionResult.PASS) return result;

        // We special case some items which we allow to place "normally". Yes, this is very ugly.
        var item = stack.getItem();
        if (item.getUseDuration(stack) == 0) {
            return turtlePlayer.player().gameMode.useItem(turtlePlayer.player(), turtlePlayer.player().level, stack, InteractionHand.MAIN_HAND);
        }

        return InteractionResult.PASS;
    }

    private static void setSignText(Level world, BlockEntity tile, String message) {
        var signTile = (SignBlockEntity) tile;
        var split = Splitter.on('\n').splitToList(message);
        var firstLine = split.size() <= 2 ? 1 : 0;
        for (var i = 0; i < 4; i++) {
            if (i >= firstLine && i < firstLine + split.size()) {
                var line = split.get(i - firstLine);
                signTile.setMessage(i, line.length() > 15
                    ? Component.literal(line.substring(0, 15))
                    : Component.literal(line)
                );
            } else {
                signTile.setMessage(i, Component.literal(""));
            }
        }
        signTile.setChanged();
        world.sendBlockUpdated(tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), Block.UPDATE_ALL);
    }

    private static class ErrorMessage {
        @Nullable
        String message;
    }
}
