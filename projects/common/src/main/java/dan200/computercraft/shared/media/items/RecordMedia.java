// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.media.IMedia;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

import javax.annotation.Nullable;

/**
 * An implementation of {@link IMedia} for items with a {@link JukeboxSong}.
 */
public final class RecordMedia implements IMedia {
    public static final RecordMedia INSTANCE = new RecordMedia();

    private RecordMedia() {
    }

    @Override
    public @Nullable String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        var song = getAudio(registries, stack);
        return song == null ? null : song.value().description().getString();
    }
}
