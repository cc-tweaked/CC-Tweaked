// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ComputerItem extends BlockItem {
    public ComputerItem(AbstractComputerBlock<?> block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag options) {
        if (options.isAdvanced() || !stack.has(DataComponents.CUSTOM_NAME)) {
            var id = stack.get(ModRegistry.DataComponents.COMPUTER_ID.get());
            if (id != null) {
                list.add(Component.translatable("gui.computercraft.tooltip.computer_id", id.id())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
