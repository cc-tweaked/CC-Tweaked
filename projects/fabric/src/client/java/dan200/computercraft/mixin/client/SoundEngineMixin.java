// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import dan200.computercraft.client.sound.SpeakerManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

@Mixin(SoundEngine.class)
class SoundEngineMixin {
    @Nullable
    @Unique
    private static SoundEngine self;

    @Inject(method = "play", at = @At(value = "HEAD"))
    @SuppressWarnings("unused")
    private void playSound(SoundInstance sound, CallbackInfo ci) {
        self = (SoundEngine) (Object) this;
    }

    @Inject(at = @At("TAIL"), method = "method_19755")
    @SuppressWarnings("unused")
    private static void onStream(AudioStream stream, Channel channel, CallbackInfo ci) {
        SpeakerManager.onPlayStreaming(assertNonNull(self), channel, stream);
    }
}
