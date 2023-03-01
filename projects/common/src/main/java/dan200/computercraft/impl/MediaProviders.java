// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaProvider;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class MediaProviders {
    private static final Logger LOG = LoggerFactory.getLogger(MediaProviders.class);

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
            try {
                var media = mediaProvider.getMedia(stack);
                if (media != null) return media;
            } catch (Exception e) {
                // Mod misbehaved, ignore it
                LOG.error("Media provider " + mediaProvider + " errored.", e);
            }
        }
        return null;
    }
}
