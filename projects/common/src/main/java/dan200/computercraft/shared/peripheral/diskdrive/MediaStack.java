// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.impl.MediaProviders;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * An immutable snapshot of the current disk. This allows us to read the stack in a thread-safe manner.
 *
 * @param stack An immutable {@link ItemStack}.
 * @param media The associated {@link IMedia} instance for this stack.
 */
record MediaStack(ItemStack stack, @Nullable IMedia media) {
    static final MediaStack EMPTY = new MediaStack(ItemStack.EMPTY, null);

    static MediaStack of(ItemStack stack) {
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
