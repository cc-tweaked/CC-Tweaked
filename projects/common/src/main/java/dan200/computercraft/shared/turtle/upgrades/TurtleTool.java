// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.core.util.Nullability;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;

public class TurtleTool extends AbstractTurtleUpgrade {
    protected static final TurtleCommandResult UNBREAKABLE = TurtleCommandResult.failure("Cannot break unbreakable block");
    protected static final TurtleCommandResult INEFFECTIVE = TurtleCommandResult.failure("Cannot break block with this tool");

    final ItemStack item;
    final float damageMulitiplier;
    final boolean allowsEnchantments;
    final TurtleToolDurability consumesDurability;
    final @Nullable TagKey<Block> breakable;

    public TurtleTool(
        ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem, float damageMulitiplier,
        boolean allowsEnchantments, TurtleToolDurability consumesDurability, @Nullable TagKey<Block> breakable
    ) {
        super(id, TurtleUpgradeType.TOOL, adjective, new ItemStack(craftItem));
        item = toolItem;
        this.damageMulitiplier = damageMulitiplier;
        this.allowsEnchantments = allowsEnchantments;
        this.consumesDurability = consumesDurability;
        this.breakable = breakable;
    }

    @Override
    public boolean isItemSuitable(ItemStack stack) {
        if (consumesDurability == TurtleToolDurability.NEVER && stack.isDamaged()) return false;
        if (!allowsEnchantments && isEnchanted(stack)) return false;
        return true;
    }

    private static boolean isEnchanted(ItemStack stack) {
        return !stack.isEmpty() && isEnchanted(stack.getTag());
    }

    private static boolean isEnchanted(@Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return false;
        return (tag.contains(ItemStack.TAG_ENCH, TAG_LIST) && !tag.getList(ItemStack.TAG_ENCH, TAG_COMPOUND).isEmpty())
               || (tag.contains("AttributeModifiers", TAG_LIST) && !tag.getList("AttributeModifiers", TAG_COMPOUND).isEmpty());
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

    private ItemStack getToolStack(ITurtleAccess turtle, TurtleSide side) {
        var item = getCraftingItem();
        var tag = turtle.getUpgradeNBTData(side);
        if (!tag.isEmpty()) item.setTag(tag);
        return item.copy();
    }

    private void setToolStack(ITurtleAccess turtle, TurtleSide side, ItemStack stack) {
        var tag = turtle.getUpgradeNBTData(side);

        var useDurability = switch (consumesDurability) {
            case NEVER -> false;
            case WHEN_ENCHANTED -> isEnchanted(tag);
            case ALWAYS -> true;
        };
        if (!useDurability) return;

        // If the tool has broken, remove the upgrade!
        if (stack.isEmpty()) {
            turtle.setUpgradeWithData(side, null);
            return;
        }

        // If the tool has changed, no clue what's going on.
        if (stack.getItem() != item.getItem()) return;

        var itemTag = stack.getTag();

        // Early return if the item hasn't changed to avoid redundant syncs with the client.
        if ((itemTag == null && tag.isEmpty()) || Objects.equals(itemTag, tag)) return;

        if (itemTag == null) {
            tag.getAllKeys().clear();
        } else {
            for (var key : itemTag.getAllKeys()) tag.put(key, Nullability.assertNonNull(itemTag.get(key)));
            tag.getAllKeys().removeIf(x -> !itemTag.contains(x));
        }

        turtle.updateUpgradeNBTData(side);
    }

    private <T> T withEquippedItem(ITurtleAccess turtle, TurtleSide side, Direction direction, Function<TurtlePlayer, T> action) {
        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, turtle.getPosition(), direction);
        turtlePlayer.loadInventory(getToolStack(turtle, side));

        var result = action.apply(turtlePlayer);

        setToolStack(turtle, side, turtlePlayer.player().getItemInHand(InteractionHand.MAIN_HAND));
        turtlePlayer.player().getInventory().clearContent();

        return result;
    }

    @Override
    public TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, Direction direction) {
        return switch (verb) {
            case ATTACK -> attack(turtle, side, direction);
            case DIG -> dig(turtle, side, direction);
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
     * Attack an entity.
     *
     * @param turtle    The current turtle.
     * @param side      The side the tool is on.
     * @param direction The direction we're attacking in.
     * @return Whether an attack occurred.
     */
    private TurtleCommandResult attack(ITurtleAccess turtle, TurtleSide side, Direction direction) {
        // Create a fake player, and orient it appropriately
        var world = turtle.getLevel();
        var position = turtle.getPosition();

        final var turtlePlayer = TurtlePlayer.getWithPosition(turtle, position, direction);

        // See if there is an entity present
        var player = turtlePlayer.player();
        var turtlePos = player.position();
        var rayDir = player.getViewVector(1.0f);
        var hit = WorldUtil.clip(world, turtlePos, rayDir, 1.5, null);
        var attacked = false;
        if (hit instanceof EntityHitResult entityHit) {
            // Load up the turtle's inventory
            var stack = getToolStack(turtle, side);
            turtlePlayer.loadInventory(stack);

            var hitEntity = entityHit.getEntity();

            // Start claiming entity drops
            DropConsumer.set(hitEntity, TurtleUtil.dropConsumer(turtle));

            // Attack the entity
            var result = PlatformHelper.get().canAttackEntity(player, hitEntity);
            if (result.consumesAction()) {
                attacked = true;
            } else if (result == InteractionResult.PASS && hitEntity.isAttackable() && !hitEntity.skipAttackInteraction(player)) {
                attacked = attack(player, direction, hitEntity);
            }

            // Stop claiming drops
            TurtleUtil.stopConsuming(turtle);

            // Put everything we collected into the turtles inventory.
            setToolStack(turtle, side, player.getItemInHand(InteractionHand.MAIN_HAND));
            player.getInventory().clearContent();
        }

        return attacked ? TurtleCommandResult.success() : TurtleCommandResult.failure("Nothing to attack here");
    }

    /**
     * Attack an entity. This is a copy of {@link Player#attack(Entity)}, with some unwanted features removed (sweeping
     * edge). This is a little limited.
     * <p>
     * Ideally we'd use attack directly (if other mods mixin to that method, we won't support their features).
     * Unfortunately,that doesn't give us any feedback to whether the attack occurred or not (and we don't want to play
     * sounds/particles).
     *
     * @param player    The fake player doing the attacking.
     * @param direction The direction the turtle is attacking.
     * @param entity    The entity to attack.
     * @return Whether we attacked or not.
     * @see Player#attack(Entity)
     */
    private boolean attack(ServerPlayer player, Direction direction, Entity entity) {
        var baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMulitiplier;
        var bonusDamage = EnchantmentHelper.getDamageBonus(
            player.getItemInHand(InteractionHand.MAIN_HAND), entity instanceof LivingEntity target ? target.getMobType() : MobType.UNDEFINED
        );
        var damage = baseDamage + bonusDamage;
        if (damage <= 0) return false;

        var knockBack = EnchantmentHelper.getKnockbackBonus(player);

        // We follow the logic in Player.attack of setting the entity on fire before attacking, so it's burning when it
        // (possibly) dies.
        var fireAspect = EnchantmentHelper.getFireAspect(player);
        var onFire = false;
        if (entity instanceof LivingEntity target && fireAspect > 0 && !target.isOnFire()) {
            onFire = true;
            target.setSecondsOnFire(1);
        }

        var source = player.damageSources().playerAttack(player);
        if (!entity.hurt(source, damage)) {
            // If we failed to damage the entity, undo us setting the entity on fire.
            if (onFire) entity.clearFire();
            return false;
        }

        // Special case for armor stands: attack twice to guarantee destroy
        if (entity.isAlive() && entity instanceof ArmorStand) entity.hurt(source, damage);

        // Apply knockback
        if (knockBack > 0) {
            if (entity instanceof LivingEntity target) {
                target.knockback(knockBack * 0.5, -direction.getStepX(), -direction.getStepZ());
            } else {
                entity.push(direction.getStepX() * knockBack * 0.5, 0.1, direction.getStepZ() * knockBack * 0.5);
            }
        }

        // Apply remaining enchantments
        if (entity instanceof LivingEntity target) EnchantmentHelper.doPostHurtEffects(target, player);
        EnchantmentHelper.doPostDamageEffects(player, entity);

        // Damage the original item stack.
        if (entity instanceof LivingEntity target) {
            player.getItemInHand(InteractionHand.MAIN_HAND).hurtEnemy(target, player);
        }

        // Apply fire aspect
        if (entity instanceof LivingEntity target && fireAspect > 0 && !target.isOnFire()) {
            target.setSecondsOnFire(4 * fireAspect);
        }

        return true;
    }

    private TurtleCommandResult dig(ITurtleAccess turtle, TurtleSide side, Direction direction) {
        var level = (ServerLevel) turtle.getLevel();

        var blockPosition = turtle.getPosition().relative(direction);
        if (level.isEmptyBlock(blockPosition) || WorldUtil.isLiquidBlock(level, blockPosition)) {
            return TurtleCommandResult.failure("Nothing to dig here");
        }

        return withEquippedItem(turtle, side, direction, turtlePlayer -> {
            var stack = turtlePlayer.player().getItemInHand(InteractionHand.MAIN_HAND);

            // Right-click the block when using a shovel/hoe.
            if (PlatformHelper.get().hasToolUsage(item) && TurtlePlaceCommand.deploy(stack, turtle, turtlePlayer, direction, null, null)) {
                return TurtleCommandResult.success();
            }

            // Check if we can break the block
            var breakable = checkBlockBreakable(level, blockPosition, turtlePlayer);
            if (!breakable.isSuccess()) return breakable;

            // And break it!
            DropConsumer.set(level, blockPosition, TurtleUtil.dropConsumer(turtle));
            var broken = !turtlePlayer.isBlockProtected(level, blockPosition) && turtlePlayer.player().gameMode.destroyBlock(blockPosition);
            TurtleUtil.stopConsuming(turtle);

            return broken ? TurtleCommandResult.success() : TurtleCommandResult.failure("Cannot break protected block");
        });
    }

    private static boolean isTriviallyBreakable(BlockGetter reader, BlockPos pos, BlockState state) {
        return
            state.is(ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE)
            // Allow breaking any "instabreak" block.
            || state.getDestroySpeed(reader, pos) == 0;
    }
}
