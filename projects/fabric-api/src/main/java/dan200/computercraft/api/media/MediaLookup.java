// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.media;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@linkplain ItemApiLookup Item API lookup} for {@link IMedia}.
 * <p>
 * The returned {@link IMedia} instance should be a singleton, and not reference the passed {@link ItemStack}.
 */
public final class MediaLookup {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "media");

    private static final ItemApiLookup<IMedia, @Nullable Void> lookup = ItemApiLookup.get(ID, IMedia.class, Void.class);

    private MediaLookup() {
    }

    public static ItemApiLookup<IMedia, @Nullable Void> get() {
        return lookup;
    }
}
