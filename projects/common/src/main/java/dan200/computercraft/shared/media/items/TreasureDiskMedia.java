// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.core.filesystem.SubMount;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * An {@link IMedia} instance for {@linkplain TreasureDiskItem treasure disks}.
 */
public final class TreasureDiskMedia implements IMedia {
    public static final IMedia INSTANCE = new TreasureDiskMedia();

    private TreasureDiskMedia() {
    }

    @Override
    public String getLabel(HolderLookup.Provider registries, ItemStack stack) {
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
