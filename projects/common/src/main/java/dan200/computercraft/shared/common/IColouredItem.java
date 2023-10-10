// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import net.minecraft.world.item.ItemStack;

public interface IColouredItem {
    String NBT_COLOUR = "Color";

    default int getColour(ItemStack stack) {
        return getColourBasic(stack);
    }

    default ItemStack withColour(ItemStack stack, int colour) {
        var copy = stack.copy();
        setColourBasic(copy, colour);
        return copy;
    }

    static int getColourBasic(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(NBT_COLOUR) ? tag.getInt(NBT_COLOUR) : -1;
    }

    static void setColourBasic(ItemStack stack, int colour) {
        if (colour == -1) {
            var tag = stack.getTag();
            if (tag != null) tag.remove(NBT_COLOUR);
        } else {
            stack.getOrCreateTag().putInt(NBT_COLOUR, colour);
        }
    }
}
