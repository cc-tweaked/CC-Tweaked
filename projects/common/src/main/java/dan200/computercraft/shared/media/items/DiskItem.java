// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.util.List;

public class DiskItem extends Item implements IMedia {
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

    @Override
    public @Nullable String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        var label = stack.get(DataComponents.CUSTOM_NAME);
        return label != null ? label.getString() : null;
    }

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        stack.set(DataComponents.CUSTOM_NAME, label != null ? Component.literal(label) : null);
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var diskID = NonNegativeId.getOrCreate(level.getServer(), stack, ModRegistry.DataComponents.DISK_ID.get(), "disk");
        return ComputerCraftAPI.createSaveDirMount(level.getServer(), "disk/" + diskID, Config.floppySpaceLimit);
    }

    public static int getDiskID(ItemStack stack) {
        return NonNegativeId.getId(stack.get(ModRegistry.DataComponents.DISK_ID.get()));
    }

    public static int getColour(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, Colour.WHITE.getARGB());
    }
}
