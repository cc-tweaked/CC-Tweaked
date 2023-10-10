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
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.util.List;

public class DiskItem extends Item implements IMedia, IColouredItem {
    private static final String NBT_ID = "DiskId";

    public DiskItem(Properties settings) {
        super(settings);
    }

    public static ItemStack createFromIDAndColour(int id, @Nullable String label, int colour) {
        var stack = new ItemStack(ModRegistry.Items.DISK.get());
        setDiskID(stack, id);
        ModRegistry.Items.DISK.get().setLabel(stack, label);
        IColouredItem.setColourBasic(stack, colour);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag options) {
        if (options.isAdvanced()) {
            var id = getDiskID(stack);
            if (id >= 0) {
                list.add(Component.translatable("gui.computercraft.tooltip.disk_id", id)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @ForgeOverride
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public @Nullable String getLabel(ItemStack stack) {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        if (label != null) {
            stack.setHoverName(Component.literal(label));
        } else {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var diskID = getDiskID(stack);
        if (diskID < 0) {
            diskID = ComputerCraftAPI.createUniqueNumberedSaveDir(level.getServer(), "disk");
            setDiskID(stack, diskID);
        }
        return ComputerCraftAPI.createSaveDirMount(level.getServer(), "disk/" + diskID, Config.floppySpaceLimit);
    }

    public static int getDiskID(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
    }

    private static void setDiskID(ItemStack stack, int id) {
        if (id >= 0) stack.getOrCreateTag().putInt(NBT_ID, id);
    }

    @Override
    public int getColour(ItemStack stack) {
        var colour = IColouredItem.getColourBasic(stack);
        return colour == -1 ? Colour.WHITE.getHex() : colour;
    }
}
