/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface ITurtleItem extends IComputerItem, IColouredItem
{
    ITurtleUpgrade getUpgrade( ItemStack stack, TurtleSide side );

    int getFuelLevel( ItemStack stack );

    Identifier getOverlay( ItemStack stack );
}
