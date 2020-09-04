/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import javax.annotation.Nonnull;

import dan200.computercraft.api.media.IMedia;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.TranslatableText;

/**
 * An implementation of IMedia for ItemRecords.
 */
public final class RecordMedia implements IMedia {
    public static final RecordMedia INSTANCE = new RecordMedia();

    private RecordMedia() {
    }

    @Override
    public String getLabel(@Nonnull ItemStack stack) {
        return this.getAudioTitle(stack);
    }

    @Override
    public String getAudioTitle(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof MusicDiscItem)) {
            return null;
        }

        return new TranslatableText(item.getTranslationKey() + ".desc").getString();
    }

    @Override
    public SoundEvent getAudio(@Nonnull ItemStack stack) {
        return ((MusicDiscItem) stack.getItem()).getSound();
    }
}
