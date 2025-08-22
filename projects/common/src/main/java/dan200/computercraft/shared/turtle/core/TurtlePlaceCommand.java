// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import com.google.common.base.Splitter;
import dan200.computercraft.api.ComputerCraftTags;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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

    private static boolean deploy(
        ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction,
        @Nullable Object[] extraArguments, @Nullable ErrorMessage outErrorMessage
    ) {
        // Parse orientation parameters once
        var orientationParams = parseOrientationParameters(extraArguments, turtle.getDirection());
        
        // Deploy on an entity
        if (deployOnEntity(turtle, turtlePlayer)) return true;

        var position = turtle.getPosition();
        var newPosition = position.relative(direction);

        if (orientationParams.isGroundAttachment) {
            // For ground torches, try to place on top of the block below first
            return (direction.getAxis() != Direction.Axis.Y && deployOnBlock(stack, turtle, turtlePlayer, newPosition.below(), Direction.UP, orientationParams, extraArguments, false, outErrorMessage))
                // Then try normal placement if that fails
                || deployOnBlock(stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), orientationParams, extraArguments, true, outErrorMessage)
                || deployOnBlock(stack, turtle, turtlePlayer, newPosition.relative(direction), direction.getOpposite(), orientationParams, extraArguments, false, outErrorMessage)
                || deployOnBlock(stack, turtle, turtlePlayer, position, direction, orientationParams, extraArguments, false, outErrorMessage);
        } else {
            if (orientationParams.clickFace != null) {
                // Click on the specified face of the front block
                var targetPos = newPosition.relative(orientationParams.clickFace);
                if (deployOnBlock(stack, turtle, turtlePlayer, targetPos, orientationParams.clickFace.getOpposite(), orientationParams, extraArguments, false, outErrorMessage)) {
                    return true;
                }
                // If that fails, fall back to normal placement
            } else if (orientationParams.blockFacing != null) {
                // For directional placement, try to click on the face opposite to the desired direction
                var clickFace = orientationParams.blockFacing.getOpposite();
                var clickPos = newPosition.relative(clickFace);

                // Try placing by clicking the appropriate face
                if (deployOnBlock(stack, turtle, turtlePlayer, clickPos, clickFace.getOpposite(), orientationParams, extraArguments, false, outErrorMessage)) {
                    return true;
                }
                // If that fails, fall back to normal placement
            }

            // Try to deploy against a block. Tries the following options:
            //     Deploy on the block immediately in front
            return deployOnBlock(stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), orientationParams, extraArguments, true, outErrorMessage)
                // Deploy on the block one block away
                || deployOnBlock(stack, turtle, turtlePlayer, newPosition.relative(direction), direction.getOpposite(), orientationParams, extraArguments, false, outErrorMessage)
                // Deploy down on the block in front
                || (direction.getAxis() != Direction.Axis.Y && deployOnBlock(stack, turtle, turtlePlayer, newPosition.below(), Direction.UP, orientationParams, extraArguments, false, outErrorMessage))
                // Deploy back onto the turtle
                || deployOnBlock(stack, turtle, turtlePlayer, position, direction, orientationParams, extraArguments, false, outErrorMessage);
        }
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

    /**
     * Calculate where a turtle would interact with a block.
     *
     * @param position The position of the block.
     * @param side     The side the turtle is clicking on.
     * @return The hit result.
     */
    public static BlockHitResult getHitResult(BlockPos position, Direction side) {
        var hitX = 0.5 + side.getStepX() * 0.5;
        var hitY = 0.5 + side.getStepY() * 0.5;
        var hitZ = 0.5 + side.getStepZ() * 0.5;
        if (Math.abs(hitY - 0.5) < 0.01) hitY = 0.45;

        return new BlockHitResult(new Vec3(position.getX() + hitX, position.getY() + hitY, position.getZ() + hitZ), side, position, false);
    }

    private static boolean deployOnBlock(
        ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side,
        OrientationParameters orientationParams, @Nullable Object[] extraArguments, boolean adjacent, @Nullable ErrorMessage outErrorMessage
    ) {
        // Re-orient the fake player
        var playerDir = side.getOpposite();
        var playerPosition = position.relative(side);

        // If we found a valid directional orientation, adjust player direction for directional blocks
        if (orientationParams.blockFacing != null) {
            // For horizontal directions, face the opposite direction
            // For vertical directions (up/down), keep the original side direction
            if (orientationParams.blockFacing.getAxis() != Direction.Axis.Y) {
                playerDir = orientationParams.blockFacing.getOpposite();
            }
            // For up/down, we keep the original playerDir since turtle's orientation doesn't affect vertical facing
        }

        turtlePlayer.setPosition(turtle, playerPosition, playerDir);

        // Check if there's something suitable to place onto
        var hit = getHitResult(position, side);
        var context = new UseOnContext(turtlePlayer.player(), InteractionHand.MAIN_HAND, hit);
        if (!canDeployOnBlock(new BlockPlaceContext(context), turtle, turtlePlayer, position, side, adjacent, outErrorMessage)) {
            return false;
        }

        var item = stack.getItem();
        var existingTile = turtle.getLevel().getBlockEntity(position);

        var placed = doDeployOnBlock(stack, turtlePlayer, hit, adjacent, orientationParams).consumesAction();

        // Modify block state after placement if we have orientation parameters
        if (placed && orientationParams.foundOrientationParam) {
            var world = turtle.getLevel();
            var placedPos = position.relative(side); // The position where the block was actually placed

            // Try the placement position first, then the original position
            if (world.isEmptyBlock(placedPos)) {
                placedPos = position;
            }

            var currentState = world.getBlockState(placedPos);
            var newState = applyOrientationToBlockState(currentState, orientationParams);

            if (newState != currentState) {
                world.setBlock(placedPos, newState, Block.UPDATE_ALL);
            }
        }

        // Set text on signs
        if (placed && item instanceof SignItem && extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String message) {
            var world = turtle.getLevel();
            var tile = world.getBlockEntity(position);
            if (tile == null || tile == existingTile) {
                tile = world.getBlockEntity(position.relative(side));
            }

            if (tile instanceof SignBlockEntity sign) setSignText(world, sign, message);
        }

        return placed;
    }

    /**
     * Attempt to place an item into the world. Returns true/false if an item was placed.
     *
     * @param stack        The stack the player is using.
     * @param turtlePlayer The player which represents the turtle
     * @param hit          Where the block we're placing against was clicked.
     * @param adjacent     If the block is directly adjacent to the turtle, and so can be interacted with via
     *                     {@link BlockState#use(Level, Player, InteractionHand, BlockHitResult)}.
     * @param orientationParams The parsed orientation parameters for block placement
     * @return If this item was deployed.
     */
    private static InteractionResult doDeployOnBlock(ItemStack stack, TurtlePlayer turtlePlayer, BlockHitResult hit, boolean adjacent,
                                                     OrientationParameters orientationParams) {
        var result = PlatformHelper.get().useOn(turtlePlayer.player(), stack, hit);
        if (result instanceof PlatformHelper.UseOnResult.Handled handled) {
            if (handled.result() != InteractionResult.PASS) return handled.result();
        } else {
            var canUse = (PlatformHelper.UseOnResult.Continue) result;

            var player = turtlePlayer.player();
            var block = player.level().getBlockState(hit.getBlockPos());
            if (adjacent && canUse.block() && block.is(ComputerCraftTags.Blocks.TURTLE_CAN_USE)) {
                var useResult = block.use(player.level(), player, InteractionHand.MAIN_HAND, hit);
                if (useResult.consumesAction()) return useResult;
            }

            var useOnResult = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (useOnResult != InteractionResult.PASS) return useOnResult;
        }

        var level = turtlePlayer.player().level();

        // We special case some items which we allow to place "normally". Yes, this is very ugly.
        var item = stack.getItem();
        if (item instanceof BucketItem || item instanceof PlaceOnWaterBlockItem || stack.is(ComputerCraftTags.Items.TURTLE_CAN_PLACE)) {
            return turtlePlayer.player().gameMode.useItem(turtlePlayer.player(), level, stack, InteractionHand.MAIN_HAND);
        }

        return InteractionResult.PASS;
    }

    private static void setSignText(Level world, SignBlockEntity sign, String message) {
        var lines = Splitter.on('\n').splitToList(message);
        var firstLine = lines.size() <= 2 ? 1 : 0;

        var signText = new SignText();
        for (int i = 0, len = Math.min(lines.size(), 4); i < len; i++) {
            var line = lines.get(i);
            signText = signText.setMessage(i + firstLine, line.length() > 15
                ? Component.literal(line.substring(0, 15))
                : Component.literal(line)
            );
        }
        sign.setText(signText, true);
        world.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Apply orientation parameters to a block state after placement.
     */
    private static BlockState applyOrientationToBlockState(BlockState state, OrientationParameters orientationParams) {
        var newState = state;

        // Handle directional facing for blocks
        if (orientationParams.blockFacing != null) {
            // Try FACING first (supports all 6 directions) - used by shulker boxes, dispensers, etc.
            if (newState.hasProperty(BlockStateProperties.FACING)) {
                // Check if the FACING property accepts this direction value
                var facingProperty = newState.getBlock().getStateDefinition().getProperty("facing");
                if (facingProperty != null && facingProperty.getPossibleValues().contains(orientationParams.blockFacing)) {
                    newState = newState.setValue(BlockStateProperties.FACING, orientationParams.blockFacing);
                }
            } 
            // Try HORIZONTAL_FACING for horizontal directions only - used by stairs, furnaces, etc.
            else if (orientationParams.blockFacing.getAxis() != Direction.Axis.Y && newState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                newState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING, orientationParams.blockFacing);
            }
            // Try AXIS for logs and pillars
            else if (newState.hasProperty(BlockStateProperties.AXIS)) {
                newState = newState.setValue(BlockStateProperties.AXIS, orientationParams.blockFacing.getAxis());
            }
        }

        // Handle slab type (top/bottom)
        if (orientationParams.isTop != null && newState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            newState = newState.setValue(BlockStateProperties.SLAB_TYPE, orientationParams.isTop ? SlabType.TOP : SlabType.BOTTOM);
        }

        // Handle stair half (top/bottom for upside-down)
        if (orientationParams.isUpsideDown && newState.hasProperty(BlockStateProperties.HALF)) {
            newState = newState.setValue(BlockStateProperties.HALF, Half.TOP);
        }

        return newState;
    }

    private static final class ErrorMessage {
        @Nullable
        String message;
    }

    private static final class OrientationParameters {
        @Nullable Direction blockFacing;
        @Nullable Boolean isTop; // null means no slab preference, true = top, false = bottom
        boolean isUpsideDown;
        boolean isGroundAttachment; // for torches on ground vs wall
        boolean foundOrientationParam;
        @Nullable Direction clickFace; // Which face to click on target block

        OrientationParameters(@Nullable Direction blockFacing, @Nullable Boolean isTop, boolean isUpsideDown, boolean isGroundAttachment, boolean foundOrientationParam, @Nullable Direction clickFace) {
            this.blockFacing = blockFacing;
            this.isTop = isTop;
            this.isUpsideDown = isUpsideDown;
            this.isGroundAttachment = isGroundAttachment;
            this.foundOrientationParam = foundOrientationParam;
            this.clickFace = clickFace;
        }
    }

    private static OrientationParameters parseOrientationParameters(@Nullable Object[] extraArguments, Direction turtleFacing) {
        Direction blockFacing = null;
        Boolean isTop = null; // null means no slab preference, true = top, false = bottom
        boolean isUpsideDown = false;
        boolean isGroundAttachment = false; // for torches on ground vs wall
        boolean foundOrientationParam = false;
        Direction clickFace = null;

        if (extraArguments != null && extraArguments.length > 0) {
            // Process all string arguments as potential orientation parameters
            for (var arg : extraArguments) {
                if (arg instanceof String orientation) {
                    switch (orientation.toLowerCase()) {
                        // Absolute cardinal directions
                        case "north" -> {
                            blockFacing = Direction.NORTH;
                            foundOrientationParam = true;
                        }
                        case "south" -> {
                            blockFacing = Direction.SOUTH;
                            foundOrientationParam = true;
                        }
                        case "east" -> {
                            blockFacing = Direction.EAST;
                            foundOrientationParam = true;
                        }
                        case "west" -> {
                            blockFacing = Direction.WEST;
                            foundOrientationParam = true;
                        }
                        case "up" -> {
                            clickFace = Direction.UP;
                            foundOrientationParam = true;
                        }
                        case "down" -> {
                            clickFace = Direction.DOWN;
                            foundOrientationParam = true;
                        }
                        // Vertical facing for blocks that support it (like dispensers)
                        case "face_up" -> {
                            blockFacing = Direction.UP;
                            foundOrientationParam = true;
                        }
                        case "face_down" -> {
                            blockFacing = Direction.DOWN;
                            foundOrientationParam = true;
                        }
                        // Relative directions based on turtle facing
                        case "left" -> {
                            blockFacing = turtleFacing.getClockWise();
                            foundOrientationParam = true;
                        }
                        case "right" -> {
                            blockFacing = turtleFacing.getCounterClockWise();
                            foundOrientationParam = true;
                        }
                        case "back", "backward" -> {
                            blockFacing = turtleFacing.getOpposite();
                            foundOrientationParam = true;
                        }
                        // Slab positioning
                        case "top" -> {
                            isTop = true;
                            foundOrientationParam = true;
                        }
                        case "bottom" -> {
                            isTop = false;
                            foundOrientationParam = true;
                        }
                        case "upside_down" -> {
                            isUpsideDown = true;
                            foundOrientationParam = true;
                        }
                        case "ground" -> {
                            isGroundAttachment = true;
                            foundOrientationParam = true;
                        }
                    }
                }
            }
        }

        return new OrientationParameters(blockFacing, isTop, isUpsideDown, isGroundAttachment, foundOrientationParam, clickFace);
    }
}
