/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.items;

import javax.annotation.Nonnull;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.util.RecordUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;

/**
 * An implementation of IMedia for ItemRecord's
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
        return RecordUtil.getRecordInfo(stack);
    }

    @Override
    public SoundEvent getAudio(@Nonnull ItemStack stack) {
        return ((MusicDiscItem) stack.getItem()).getSound();
    }
}
