/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.DyeColor;

import javax.annotation.Nullable;

public final class ColourUtils
{
    @Nullable
    private ColourUtils() {}

    public static DyeColor getStackColour(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DyeItem ? ((DyeItem) item).getColor() : null;
    }
}
