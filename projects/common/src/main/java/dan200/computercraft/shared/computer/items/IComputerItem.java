// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
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

    ComputerFamily getFamily();

    ItemStack withFamily(ItemStack stack, ComputerFamily family);
}
