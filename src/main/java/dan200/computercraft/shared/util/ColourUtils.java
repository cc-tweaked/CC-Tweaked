/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import javax.annotation.Nullable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ColourUtils
{
    @Nullable
    private ColourUtils() {}

    public static DyeColor getStackColour( ItemStack stack )
    {
        Item item = stack.getItem();
        return item instanceof DyeItem ? ((DyeItem) item).getDyeColor() : null;
    }
}
