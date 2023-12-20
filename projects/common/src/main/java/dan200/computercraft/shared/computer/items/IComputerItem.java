// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IComputerItem {
    String NBT_ID = "ComputerId";

    default int getComputerID(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
    }

    default @Nullable String getLabel(ItemStack stack) {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }

    /**
     * Create a new stack, changing the underlying item.
     * <p>
     * This should copy the computer's data to a different item of the same type (for instance, converting a normal
     * computer to an advanced one).
     *
     * @param stack   The current computer stack.
     * @param newItem The new item.
     * @return The new stack, possibly {@linkplain ItemStack#EMPTY empty} if {@code newItem} is of the same type.
     */
    ItemStack changeItem(ItemStack stack, Item newItem);
}
