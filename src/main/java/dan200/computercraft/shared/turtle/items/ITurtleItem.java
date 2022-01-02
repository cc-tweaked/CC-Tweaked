/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ITurtleItem extends IComputerItem, IColouredItem
{
    @Nullable
    ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, @Nonnull TurtleSide side );

    int getFuelLevel( @Nonnull ItemStack stack );

    @Nullable
    ResourceLocation getOverlay( @Nonnull ItemStack stack );
}
