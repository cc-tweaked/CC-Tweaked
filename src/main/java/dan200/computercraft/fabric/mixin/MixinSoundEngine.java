/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

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

@Mixin( SoundEngine.class )
public class MixinSoundEngine
{
    // Used to capture the SoundInstance argument passed to SoundEngine#play. Not a thread-safe way to do it but
    // this code is only called from the render thread as far as I can tell.
    @Unique
    private static SoundInstance onPlaySoundInstanceCapture;

    @Inject(
        method = "lambda$play$8",
        at = @At( "HEAD" ),
        cancellable = true
    )
    private static void onStreamingSourcePlay( AudioStream audioStream, Channel channel, CallbackInfo ci )
    {
        if( SpeakerManager.playStreaming( onPlaySoundInstanceCapture, channel ) ) ci.cancel();
    }

    @Inject(
        method = "play",
        at = @At( "HEAD" )
    )
    void onPlay( SoundInstance soundInstance, CallbackInfo ci )
    {
        onPlaySoundInstanceCapture = soundInstance;
    }
}
