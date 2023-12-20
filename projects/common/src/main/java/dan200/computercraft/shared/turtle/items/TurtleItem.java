// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.items.AbstractComputerItem;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.turtle.core.TurtleBrain.*;

public class TurtleItem extends AbstractComputerItem implements IColouredItem {
    public TurtleItem(TurtleBlock block, Properties settings) {
        super(block, settings);
    }

    public ItemStack create(
        int id, @Nullable String label, int colour,
        @Nullable UpgradeData<ITurtleUpgrade> leftUpgrade, @Nullable UpgradeData<ITurtleUpgrade> rightUpgrade,
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
            var tag = stack.getOrCreateTag();
            tag.putString(NBT_LEFT_UPGRADE, leftUpgrade.upgrade().getUpgradeID().toString());
            if (!leftUpgrade.data().isEmpty()) tag.put(NBT_LEFT_UPGRADE_DATA, leftUpgrade.data().copy());
        }

        if (rightUpgrade != null) {
            var tag = stack.getOrCreateTag();
            tag.putString(NBT_RIGHT_UPGRADE, rightUpgrade.upgrade().getUpgradeID().toString());
            if (!rightUpgrade.data().isEmpty()) tag.put(NBT_RIGHT_UPGRADE_DATA, rightUpgrade.data().copy());
        }

        return stack;
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
    @ForgeOverride
    public String getCreatorModId(ItemStack stack) {
        // Determine our "creator mod" from the upgrades. We attempt to find the first non-vanilla/non-CC
        // upgrade (starting from the left).

        var left = getUpgrade(stack, TurtleSide.LEFT);
        if (left != null) {
            var mod = TurtleUpgrades.instance().getOwner(left);
            if (mod != null && !mod.equals(ComputerCraftAPI.MOD_ID)) return mod;
        }

        var right = getUpgrade(stack, TurtleSide.RIGHT);
        if (right != null) {
            var mod = TurtleUpgrades.instance().getOwner(right);
            if (mod != null && !mod.equals(ComputerCraftAPI.MOD_ID)) return mod;
        }

        return ComputerCraftAPI.MOD_ID;
    }

    @Override
    public ItemStack changeItem(ItemStack stack, Item newItem) {
        return newItem instanceof TurtleItem turtle ? turtle.create(
            getComputerID(stack), getLabel(stack),
            getColour(stack),
            getUpgradeWithData(stack, TurtleSide.LEFT), getUpgradeWithData(stack, TurtleSide.RIGHT),
            getFuelLevel(stack), getOverlay(stack)
        ) : ItemStack.EMPTY;
    }

    public @Nullable ITurtleUpgrade getUpgrade(ItemStack stack, TurtleSide side) {
        var tag = stack.getTag();
        if (tag == null) return null;

        var key = side == TurtleSide.LEFT ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        if (!tag.contains(key)) return null;
        return TurtleUpgrades.instance().get(tag.getString(key));
    }

    public @Nullable UpgradeData<ITurtleUpgrade> getUpgradeWithData(ItemStack stack, TurtleSide side) {
        var tag = stack.getTag();
        if (tag == null) return null;

        var key = side == TurtleSide.LEFT ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        if (!tag.contains(key)) return null;
        var upgrade = TurtleUpgrades.instance().get(tag.getString(key));
        if (upgrade == null) return null;
        var dataKey = side == TurtleSide.LEFT ? NBT_LEFT_UPGRADE_DATA : NBT_RIGHT_UPGRADE_DATA;
        return UpgradeData.of(upgrade, NBTUtil.getCompoundOrEmpty(tag, dataKey));
    }

    public @Nullable ResourceLocation getOverlay(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(NBT_OVERLAY) ? new ResourceLocation(tag.getString(NBT_OVERLAY)) : null;
    }

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
