/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class TreasureDiskItem extends Item implements IMedia {
    private static final String NBT_TITLE = "Title";
    private static final String NBT_COLOUR = "Colour";
    private static final String NBT_SUB_PATH = "SubPath";

    public TreasureDiskItem(Properties settings) {
        super(settings);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag tooltipOptions) {
        var label = getTitle(stack);
        if (!label.isEmpty()) list.add(Component.literal(label));
    }

    @ForgeOverride
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public String getLabel(ItemStack stack) {
        return getTitle(stack);
    }

    @Override
    public @Nullable IMount createDataMount(ItemStack stack, Level world) {
        var rootTreasure = getTreasureMount();
        if (rootTreasure == null) return null;

        var subPath = getSubPath(stack);
        try {
            if (rootTreasure.exists(subPath)) {
                return new SubMount(rootTreasure, subPath);
            } else if (rootTreasure.exists("deprecated/" + subPath)) {
                return new SubMount(rootTreasure, "deprecated/" + subPath);
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack create(String subPath, int colourIndex) {
        var result = new ItemStack(ModRegistry.Items.TREASURE_DISK.get());
        var nbt = result.getOrCreateTag();
        nbt.putString(NBT_SUB_PATH, subPath);

        var slash = subPath.indexOf('/');
        if (slash >= 0) {
            var author = subPath.substring(0, slash);
            var title = subPath.substring(slash + 1);
            nbt.putString(NBT_TITLE, "\"" + title + "\" by " + author);
        } else {
            nbt.putString(NBT_TITLE, "untitled");
        }
        nbt.putInt(NBT_COLOUR, Colour.values()[colourIndex].getHex());

        return result;
    }

    private static @Nullable IMount getTreasureMount() {
        return ComputerCraftAPI.createResourceMount("computercraft", "lua/treasure");
    }

    private static String getTitle(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_TITLE) ? nbt.getString(NBT_TITLE) : "'missingno' by how did you get this anyway?";
    }

    private static String getSubPath(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_SUB_PATH) ? nbt.getString(NBT_SUB_PATH) : "dan200/alongtimeago";
    }

    public static int getColour(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_COLOUR) ? nbt.getInt(NBT_COLOUR) : Colour.BLUE.getHex();
    }
}
