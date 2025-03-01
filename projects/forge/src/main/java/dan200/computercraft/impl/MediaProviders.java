// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaProvider;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class MediaProviders {
    private static final Set<MediaProvider> providers = new LinkedHashSet<>();

    private MediaProviders() {
    }

    public static synchronized void register(MediaProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        providers.add(provider);
    }

    public static @Nullable IMedia get(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // Try the handlers in order:
        for (var mediaProvider : providers) {
            var media = mediaProvider.getMedia(stack);
            if (media != null) return media;
        }
        return null;
    }
}
