/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputerBase;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import net.minecraft.core.NonNullList;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.turtle.core.TurtleBrain.*;

public class ItemTurtle extends ItemComputerBase implements ITurtleItem {
    public ItemTurtle(BlockTurtle block, Properties settings) {
        super(block, settings);
    }

    public ItemStack create(
        int id, @Nullable String label, int colour,
        @Nullable ITurtleUpgrade leftUpgrade, @Nullable ITurtleUpgrade rightUpgrade,
        int fuelLevel, @Nullable ResourceLocation overlay
    ) {
        // Build the stack
        var stack = new ItemStack(this);
        if (label != null) stack.setHoverName(Component.literal(label));
        if (id >= 0) stack.getOrCreateTag().putInt(NBT_ID, id);
        IColouredItem.setColourBasic(stack, colour);
        if (fuelLevel > 0) stack.getOrCreateTag().putInt(NBT_FUEL, fuelLevel);
        if (overlay != null) stack.getOrCreateTag().putString(NBT_OVERLAY, overlay.toString());

        if (leftUpgrade != null) {
            stack.getOrCreateTag().putString(NBT_LEFT_UPGRADE, leftUpgrade.getUpgradeID().toString());
        }

        if (rightUpgrade != null) {
            stack.getOrCreateTag().putString(NBT_RIGHT_UPGRADE, rightUpgrade.getUpgradeID().toString());
        }

        return stack;
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> list) {
        if (!allowedIn(group)) return;

        list.add(create(-1, null, -1, null, null, 0, null));
        TurtleUpgrades.getVanillaUpgrades()
            .map(x -> create(-1, null, -1, null, x, 0, null))
            .forEach(list::add);
    }

    @Override
    public Component getName(ItemStack stack) {
        var baseString = getDescriptionId(stack);
        var left = getUpgrade(stack, TurtleSide.LEFT);
        var right = getUpgrade(stack, TurtleSide.RIGHT);
        if (left != null && right != null) {
            return Component.translatable(baseString + ".upgraded_twice",
                Component.translatable(right.getUnlocalisedAdjective()),
                Component.translatable(left.getUnlocalisedAdjective())
            );
        } else if (left != null) {
            return Component.translatable(baseString + ".upgraded",
                Component.translatable(left.getUnlocalisedAdjective())
            );
        } else if (right != null) {
            return Component.translatable(baseString + ".upgraded",
                Component.translatable(right.getUnlocalisedAdjective())
            );
        } else {
            return Component.translatable(baseString);
        }
    }

    @Nullable
    @Override
    public String getCreatorModId(ItemStack stack) {
        // Determine our "creator mod" from the upgrades. We attempt to find the first non-vanilla/non-CC
        // upgrade (starting from the left).

        var left = getUpgrade(stack, TurtleSide.LEFT);
        if (left != null) {
            var mod = TurtleUpgrades.instance().getOwner(left);
            if (mod != null && !mod.equals(ComputerCraft.MOD_ID)) return mod;
        }

        var right = getUpgrade(stack, TurtleSide.RIGHT);
        if (right != null) {
            var mod = TurtleUpgrades.instance().getOwner(right);
            if (mod != null && !mod.equals(ComputerCraft.MOD_ID)) return mod;
        }

        return super.getCreatorModId(stack);
    }

    @Override
    public ItemStack withFamily(ItemStack stack, ComputerFamily family) {
        return TurtleItemFactory.create(
            getComputerID(stack), getLabel(stack),
            getColour(stack), family,
            getUpgrade(stack, TurtleSide.LEFT), getUpgrade(stack, TurtleSide.RIGHT),
            getFuelLevel(stack), getOverlay(stack)
        );
    }

    @Override
    public @Nullable ITurtleUpgrade getUpgrade(ItemStack stack, TurtleSide side) {
        var tag = stack.getTag();
        if (tag == null) return null;

        var key = side == TurtleSide.LEFT ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        return tag.contains(key) ? TurtleUpgrades.instance().get(tag.getString(key)) : null;
    }

    @Override
    public @Nullable ResourceLocation getOverlay(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(NBT_OVERLAY) ? new ResourceLocation(tag.getString(NBT_OVERLAY)) : null;
    }

    @Override
    public int getFuelLevel(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(NBT_FUEL) ? tag.getInt(NBT_FUEL) : 0;
    }

    public static final CauldronInteraction CAULDRON_INTERACTION = (blockState, level, pos, player, hand, stack) -> {
        if (IColouredItem.getColourBasic(stack) == -1) return InteractionResult.PASS;
        if (!level.isClientSide) {
            IColouredItem.setColourBasic(stack, -1);
            LayeredCauldronBlock.lowerFillLevel(blockState, level, pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    };
}
