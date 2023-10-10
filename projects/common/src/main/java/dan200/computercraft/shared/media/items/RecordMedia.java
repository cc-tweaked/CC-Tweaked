// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.api.media.IMedia;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

import javax.annotation.Nullable;

/**
 * An implementation of IMedia for ItemRecords.
 */
public final class RecordMedia implements IMedia {
    public static final RecordMedia INSTANCE = new RecordMedia();

    private RecordMedia() {
    }

    @Override
    public @Nullable String getLabel(ItemStack stack) {
        return getAudioTitle(stack);
    }

    @Override
    public @Nullable String getAudioTitle(ItemStack stack) {
        var item = stack.getItem();
        if (!(item instanceof RecordItem)) return null;

        return Component.translatable(item.getDescriptionId() + ".desc").getString();
    }

    @Override
    public @Nullable SoundEvent getAudio(ItemStack stack) {
        var item = stack.getItem();
        if (!(item instanceof RecordItem)) return null;

        return ((RecordItem) item).getSound();
    }
}
