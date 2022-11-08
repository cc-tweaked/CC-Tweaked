/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;

public class TurtleTool extends AbstractTurtleUpgrade {
    protected static final TurtleCommandResult UNBREAKABLE = TurtleCommandResult.failure("Cannot break unbreakable block");
    protected static final TurtleCommandResult INEFFECTIVE = TurtleCommandResult.failure("Cannot break block with this tool");

    final ItemStack item;
    final float damageMulitiplier;
    @Nullable
    final TagKey<Block> breakable;

    public TurtleTool(ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem, float damageMulitiplier, @Nullable TagKey<Block> breakable) {
        super(id, TurtleUpgradeType.TOOL, adjective, new ItemStack(craftItem));
        item = toolItem;
        this.damageMulitiplier = damageMulitiplier;
        this.breakable = breakable;
    }

    @Override
    public boolean isItemSuitable(@Nonnull ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if (stack.isDamaged() || stack.isEnchanted() || stack.hasCustomHoverName()) return false;
        if (tag.contains("AttributeModifiers", TAG_LIST) &&
            !tag.getList("AttributeModifiers", TAG_COMPOUND).isEmpty()) {
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction) {
        return switch (verb) {
            case ATTACK -> attack(turtle, direction);
            case DIG -> dig(turtle, direction);
        };
    }

    protected TurtleCommandResult checkBlockBreakable(BlockState state, Level world, BlockPos pos, TurtlePlayer player) {
        var block = state.getBlock();
        if (state.isAir() || block == Blocks.BEDROCK
            || state.getDestroyProgress(player, world, pos) <= 0
            || !block.canEntityDestroy(state, world, pos, player)) {
            return UNBREAKABLE;
        }

        return breakable == null || state.is(breakable) || isTriviallyBreakable(world, pos, state)
            ? TurtleCommandResult.success() : INEFFECTIVE;
    }

    private TurtleCommandResult attack(ITurtleAccess turtle, Direction direction) {
        // Create a fake player, and orient it appropriately
        var world = turtle.getLevel();
        var position = turtle.getPosition();

        final var turtlePlayer = TurtlePlayer.getWithPosition(turtle, position, direction);

        // See if there is an entity present
        var turtlePos = turtlePlayer.position();
        var rayDir = turtlePlayer.getViewVector(1.0f);
        var hit = WorldUtil.clip(world, turtlePos, rayDir, 1.5, null);
        if (hit instanceof EntityHitResult entityHit) {
            // Load up the turtle's inventory
            var stackCopy = item.copy();
            turtlePlayer.loadInventory(stackCopy);

            var hitEntity = entityHit.getEntity();

            // Fire several events to ensure we have permissions.
            if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(turtlePlayer, hitEntity)) || !hitEntity.isAttackable()) {
                return TurtleCommandResult.failure("Nothing to attack here");
            }

            // Start claiming entity drops
            DropConsumer.set(hitEntity, TurtleUtil.dropConsumer(turtle));

            // Attack the entity
            var attacked = false;
            if (!hitEntity.skipAttackInteraction(turtlePlayer)) {
                var damage = (float) turtlePlayer.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMulitiplier;
                if (damage > 0.0f) {
                    var source = DamageSource.playerAttack(turtlePlayer);
                    if (hitEntity instanceof ArmorStand) {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.hurt(source, damage);
                        if (hitEntity.isAlive()) hitEntity.hurt(source, damage);
                        attacked = true;
                    } else {
                        if (hitEntity.hurt(source, damage)) attacked = true;
                    }
                }
            }

            // Stop claiming drops
            TurtleUtil.stopConsuming(turtle);

            // Put everything we collected into the turtles inventory, then return
            if (attacked) {
                turtlePlayer.getInventory().clearContent();
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure("Nothing to attack here");
    }

    private TurtleCommandResult dig(ITurtleAccess turtle, Direction direction) {
        if (item.canPerformAction(ToolActions.SHOVEL_FLATTEN) || item.canPerformAction(ToolActions.HOE_TILL)) {
            if (TurtlePlaceCommand.deployCopiedItem(item.copy(), turtle, direction, null, null)) {
                return TurtleCommandResult.success();
            }
        }

        // Get ready to dig
        var world = turtle.getLevel();
        var turtlePosition = turtle.getPosition();

        var blockPosition = turtlePosition.relative(direction);
        if (world.isEmptyBlock(blockPosition) || WorldUtil.isLiquidBlock(world, blockPosition)) {
            return TurtleCommandResult.failure("Nothing to dig here");
        }

        var state = world.getBlockState(blockPosition);
        var fluidState = world.getFluidState(blockPosition);

        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, turtlePosition, direction);
        turtlePlayer.loadInventory(item.copy());

        if (ComputerCraft.turtlesObeyBlockProtection) {
            // Check spawn protection
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, blockPosition, state, turtlePlayer))) {
                return TurtleCommandResult.failure("Cannot break protected block");
            }

            if (!TurtlePermissions.isBlockEditable(world, blockPosition, turtlePlayer)) {
                return TurtleCommandResult.failure("Cannot break protected block");
            }
        }

        // Check if we can break the block
        var breakable = checkBlockBreakable(state, world, blockPosition, turtlePlayer);
        if (!breakable.isSuccess()) return breakable;

        // Consume the items the block drops
        DropConsumer.set(world, blockPosition, TurtleUtil.dropConsumer(turtle));

        var tile = world.getBlockEntity(blockPosition);

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.levelEvent(2001, blockPosition, Block.getId(state));

        // Destroy the block
        var canHarvest = state.canHarvestBlock(world, blockPosition, turtlePlayer);
        var canBreak = state.onDestroyedByPlayer(world, blockPosition, turtlePlayer, canHarvest, fluidState);
        if (canBreak) state.getBlock().destroy(world, blockPosition, state);
        if (canHarvest && canBreak) {
            state.getBlock().playerDestroy(world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandItem());
        }

        TurtleUtil.stopConsuming(turtle);

        return TurtleCommandResult.success();

    }

    protected boolean isTriviallyBreakable(BlockGetter reader, BlockPos pos, BlockState state) {
        return state.is(ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE)
            // Allow breaking any "instabreak" block.
            || state.getDestroySpeed(reader, pos) == 0;
    }
}
