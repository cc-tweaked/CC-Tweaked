/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.items.IComputerItem;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface ITurtleItem extends IComputerItem, IColouredItem {
    @Nullable
    ITurtleUpgrade getUpgrade(@Nonnull ItemStack stack, @Nonnull TurtleSide side);

    int getFuelLevel(@Nonnull ItemStack stack);

    @Nullable
    Identifier getOverlay(@Nonnull ItemStack stack);
}
