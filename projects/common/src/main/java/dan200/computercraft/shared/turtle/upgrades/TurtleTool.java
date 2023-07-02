// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.TurtleUtil;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;

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
    public boolean isItemSuitable(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if (stack.isDamaged() || stack.isEnchanted()) return false;
        if (tag.contains("AttributeModifiers", TAG_LIST) && !tag.getList("AttributeModifiers", TAG_COMPOUND).isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public CompoundTag getUpgradeData(ItemStack stack) {
        // Just use the current item's tag.
        var itemTag = stack.getTag();
        return itemTag == null ? new CompoundTag() : itemTag;
    }

    @Override
    public ItemStack getUpgradeItem(CompoundTag upgradeData) {
        // Copy upgrade data back to the item.
        var item = super.getUpgradeItem(upgradeData);
        if (!upgradeData.isEmpty()) item.setTag(upgradeData);
        return item;
    }

    @Override
    public TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, Direction direction) {
        return switch (verb) {
            case ATTACK -> attack(turtle, direction);
            case DIG -> dig(turtle, direction);
        };
    }

    protected TurtleCommandResult checkBlockBreakable(Level world, BlockPos pos, TurtlePlayer player) {
        var state = world.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof GameMasterBlock || state.getDestroyProgress(player.player(), world, pos) <= 0) {
            return UNBREAKABLE;
        }

        return breakable == null || state.is(breakable) || isTriviallyBreakable(world, pos, state)
            ? TurtleCommandResult.success() : INEFFECTIVE;
    }

    /**
     * Attack an entity. This is a <em>very</em> cut down version of {@link Player#attack(Entity)}, which doesn't handle
     * enchantments, knockback, etc... Unfortunately we can't call attack directly as damage calculations are rather
     * different (and we don't want to play sounds/particles).
     *
     * @param turtle    The current turtle.
     * @param direction The direction we're attacking in.
     * @return Whether an attack occurred.
     * @see Player#attack(Entity)
     */
    private TurtleCommandResult attack(ITurtleAccess turtle, Direction direction) {
        // Create a fake player, and orient it appropriately
        var world = turtle.getLevel();
        var position = turtle.getPosition();

        final var turtlePlayer = TurtlePlayer.getWithPosition(turtle, position, direction);

        // See if there is an entity present
        var player = turtlePlayer.player();
        var turtlePos = player.position();
        var rayDir = player.getViewVector(1.0f);
        var hit = WorldUtil.clip(world, turtlePos, rayDir, 1.5, null);
        if (hit instanceof EntityHitResult entityHit) {
            // Load up the turtle's inventory
            var stackCopy = item.copy();
            turtlePlayer.loadInventory(stackCopy);

            var hitEntity = entityHit.getEntity();

            // Start claiming entity drops
            DropConsumer.set(hitEntity, TurtleUtil.dropConsumer(turtle));

            // Attack the entity
            var attacked = false;
            var result = PlatformHelper.get().canAttackEntity(player, hitEntity);
            if (result.consumesAction()) {
                attacked = true;
            } else if (result == InteractionResult.PASS && hitEntity.isAttackable() && !hitEntity.skipAttackInteraction(player)) {
                var damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMulitiplier;
                if (damage > 0.0f) {
                    var source = player.damageSources().playerAttack(player);
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
            player.getInventory().clearContent();
            if (attacked) return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure("Nothing to attack here");
    }

    private TurtleCommandResult dig(ITurtleAccess turtle, Direction direction) {
        if (PlatformHelper.get().hasToolUsage(item) && TurtlePlaceCommand.deployCopiedItem(item.copy(), turtle, direction, null, null)) {
            return TurtleCommandResult.success();
        }

        var level = (ServerLevel) turtle.getLevel();
        var turtlePosition = turtle.getPosition();

        var blockPosition = turtlePosition.relative(direction);
        if (level.isEmptyBlock(blockPosition) || WorldUtil.isLiquidBlock(level, blockPosition)) {
            return TurtleCommandResult.failure("Nothing to dig here");
        }

        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, turtlePosition, direction);
        turtlePlayer.loadInventory(item.copy());

        // Check if we can break the block
        var breakable = checkBlockBreakable(level, blockPosition, turtlePlayer);
        if (!breakable.isSuccess()) return breakable;

        DropConsumer.set(level, blockPosition, TurtleUtil.dropConsumer(turtle));
        var broken = !turtlePlayer.isBlockProtected(level, blockPosition) && turtlePlayer.player().gameMode.destroyBlock(blockPosition);
        TurtleUtil.stopConsuming(turtle);

        // Check spawn protection
        return broken ? TurtleCommandResult.success() : TurtleCommandResult.failure("Cannot break protected block");
    }

    private static boolean isTriviallyBreakable(BlockGetter reader, BlockPos pos, BlockState state) {
        return
            state.is(ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE)
            // Allow breaking any "instabreak" block.
            || state.getDestroySpeed(reader, pos) == 0;
    }
}
