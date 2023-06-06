// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.ITurtleBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class TurtleItemFactory {
    private TurtleItemFactory() {
    }

    public static ItemStack create(ITurtleBlockEntity turtle) {
        var access = turtle.getAccess();

        return create(
            turtle.getComputerID(), turtle.getLabel(), turtle.getColour(), turtle.getFamily(),
            access.getUpgrade(TurtleSide.LEFT), access.getUpgrade(TurtleSide.RIGHT),
            access.getFuelLevel(), turtle.getOverlay(),
            access.getUpgradeNBTData(TurtleSide.LEFT), access.getUpgradeNBTData(TurtleSide.RIGHT)
        );
    }

    public static ItemStack create(
        int id, @Nullable String label, int colour, ComputerFamily family,
        @Nullable ITurtleUpgrade leftUpgrade, @Nullable ITurtleUpgrade rightUpgrade,
        int fuelLevel, @Nullable ResourceLocation overlay,
        @Nullable CompoundTag leftUpgradeData, @Nullable CompoundTag rightUpdateData
    ) {
        return switch (family) {
            case NORMAL ->
                ModRegistry.Items.TURTLE_NORMAL.get().create(id, label, colour, leftUpgrade, rightUpgrade, fuelLevel, overlay, leftUpgradeData, rightUpdateData);
            case ADVANCED ->
                ModRegistry.Items.TURTLE_ADVANCED.get().create(id, label, colour, leftUpgrade, rightUpgrade, fuelLevel, overlay, leftUpgradeData, rightUpdateData);
            default -> ItemStack.EMPTY;
        };
    }
}
