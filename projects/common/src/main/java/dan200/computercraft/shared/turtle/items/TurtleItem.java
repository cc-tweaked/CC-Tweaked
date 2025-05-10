// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.impl.UpgradeManager;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.jspecify.annotations.Nullable;

public class TurtleItem extends BlockItem {
    public TurtleItem(TurtleBlock block, Properties settings) {
        super(block, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        return UpgradeManager.getName(getDescriptionId(), getUpgrade(stack, TurtleSide.LEFT), getUpgrade(stack, TurtleSide.RIGHT));
    }

    @Nullable
    @ForgeOverride
    public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
        return TurtleUpgrades.instance().getOwner(getUpgradeWithData(stack, TurtleSide.LEFT), getUpgradeWithData(stack, TurtleSide.RIGHT));
    }

    public static @Nullable ITurtleUpgrade getUpgrade(ItemStack stack, TurtleSide side) {
        var upgrade = getUpgradeWithData(stack, side);
        return upgrade == null ? null : upgrade.upgrade();
    }

    public static @Nullable UpgradeData<ITurtleUpgrade> getUpgradeWithData(ItemStack stack, TurtleSide side) {
        return stack.get(switch (side) {
            case LEFT -> ModRegistry.DataComponents.LEFT_TURTLE_UPGRADE.get();
            case RIGHT -> ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get();
        });
    }

    public static @Nullable TurtleOverlay getOverlay(ItemStack stack) {
        var overlay = stack.get(ModRegistry.DataComponents.OVERLAY.get());
        return overlay == null ? null : overlay.value();
    }

    public static final CauldronInteraction CAULDRON_INTERACTION = (blockState, level, pos, player, hand, stack) -> {
        if (!stack.has(DataComponents.DYED_COLOR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide) {
            stack.remove(DataComponents.DYED_COLOR);
            LayeredCauldronBlock.lowerFillLevel(blockState, level, pos);
        }

        return InteractionResult.SUCCESS;
    };
}
