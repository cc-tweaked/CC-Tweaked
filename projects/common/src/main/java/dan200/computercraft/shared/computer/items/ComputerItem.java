// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ComputerItem extends AbstractComputerItem {
    public ComputerItem(ComputerBlock<?> block, Properties settings) {
        super(block, settings);
    }

    public ItemStack create(int id, @Nullable String label) {
        var result = new ItemStack(this);
        if (id >= 0) result.getOrCreateTag().putInt(NBT_ID, id);
        if (label != null) result.setHoverName(Component.literal(label));
        return result;
    }

    @Override
    public ItemStack withFamily(ItemStack stack, ComputerFamily family) {
        var result = ComputerItemFactory.create(getComputerID(stack), null, family);
        if (stack.hasCustomHoverName()) result.setHoverName(stack.getHoverName());
        return result;
    }
}
