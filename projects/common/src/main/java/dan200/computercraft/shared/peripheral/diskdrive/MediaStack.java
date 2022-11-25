/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.impl.MediaProviders;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * An immutable snapshot of the current disk. This allows us to read the stack in a thread-safe manner.
 */
class MediaStack {
    static final MediaStack EMPTY = new MediaStack(ItemStack.EMPTY);

    final ItemStack stack;
    final @Nullable IMedia media;

    @Nullable
    private Mount mount;

    MediaStack(ItemStack stack) {
        this.stack = stack;
        media = MediaProviders.get(stack);
    }

    @Nullable
    SoundEvent getAudio() {
        return media != null ? media.getAudio(stack) : null;
    }

    @Nullable
    String getAudioTitle() {
        return media != null ? media.getAudioTitle(stack) : null;
    }

    @Nullable
    public Mount getMount(ServerLevel level) {
        if (media == null) return null;

        if (mount == null) mount = media.createDataMount(stack, level);
        return mount;
    }
}
