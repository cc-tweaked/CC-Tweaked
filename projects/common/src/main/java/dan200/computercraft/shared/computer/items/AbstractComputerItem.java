// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlock;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class AbstractComputerItem extends BlockItem implements IMedia {
    public AbstractComputerItem(AbstractComputerBlock<?> block, Properties settings) {
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

    @Override
    public @Nullable String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        return DataComponentUtil.getCustomName(stack);
    }

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        DataComponentUtil.setCustomName(stack, label);
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var id = stack.get(ModRegistry.DataComponents.COMPUTER_ID.get());
        return id != null ? ComputerCraftAPI.createSaveDirMount(level.getServer(), "computer/" + id.id(), Config.computerSpaceLimit) : null;
    }
}
