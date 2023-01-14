/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.impl.MediaProviders;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * An immutable snapshot of the current disk. This allows us to read the stack in a thread-safe manner.
 */
final class MediaStack {
    static final MediaStack EMPTY = new MediaStack(ItemStack.EMPTY, null);

    final ItemStack stack;
    final @Nullable IMedia media;

    private MediaStack(ItemStack stack, @Nullable IMedia media) {
        this.stack = stack;
        this.media = media;
    }

    public static MediaStack of(ItemStack stack) {
        if (stack.isEmpty()) return EMPTY;

        var freshStack = stack.copy();
        return new MediaStack(freshStack, MediaProviders.get(freshStack));
    }

    @Nullable
    SoundEvent getAudio() {
        return media != null ? media.getAudio(stack) : null;
    }

    @Nullable
    String getAudioTitle() {
        return media != null ? media.getAudioTitle(stack) : null;
    }
}
