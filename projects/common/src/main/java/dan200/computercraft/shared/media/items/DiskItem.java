// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.LevelReader;

import java.util.List;

public class DiskItem extends Item {
    public DiskItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag options) {
        if (options.isAdvanced()) {
            var id = stack.get(ModRegistry.DataComponents.DISK_ID.get());
            if (id != null) {
                list.add(Component.translatable("gui.computercraft.tooltip.disk_id", id.id())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @ForgeOverride
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    public static int getDiskID(ItemStack stack) {
        return NonNegativeId.getId(stack.get(ModRegistry.DataComponents.DISK_ID.get()));
    }

    public static int getColour(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, Colour.WHITE.getARGB());
    }
}
