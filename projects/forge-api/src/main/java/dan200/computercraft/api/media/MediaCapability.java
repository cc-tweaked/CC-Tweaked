// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.media;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jspecify.annotations.Nullable;

/**
 * {@linkplain ItemCapability Item API lookup} for {@link IMedia}.
 * <p>
 * The returned {@link IMedia} instance should be a singleton, and not reference the passed {@link ItemStack}.
 */
public final class MediaCapability {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "media");

    private static final ItemCapability<IMedia, @Nullable Void> lookup = ItemCapability.createVoid(ID, IMedia.class);

    private MediaCapability() {
    }

    public static ItemCapability<IMedia, @Nullable Void> get() {
        return lookup;
    }
}
