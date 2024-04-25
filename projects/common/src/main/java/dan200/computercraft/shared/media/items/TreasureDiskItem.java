// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class TreasureDiskItem extends Item implements IMedia {
    public TreasureDiskItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipOptions) {
        list.add(Component.literal(TreasureDisk.getTitle(stack)));
    }

    @ForgeOverride
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public String getLabel(ItemStack stack) {
        return TreasureDisk.getTitle(stack);
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var rootTreasure = ComputerCraftAPI.createResourceMount(level.getServer(), "computercraft", "lua/treasure");
        if (rootTreasure == null) return null;

        var treasureDisk = stack.get(ModRegistry.DataComponents.TREASURE_DISK.get());
        if (treasureDisk == null) return null;

        var subPath = treasureDisk.path();
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
}
