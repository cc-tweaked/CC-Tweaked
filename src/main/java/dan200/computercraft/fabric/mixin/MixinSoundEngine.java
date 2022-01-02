/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.blaze3d.audio.Channel;
import dan200.computercraft.fabric.events.CustomClientEvents;
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
    // Used to capture the SoundInstance argument passed to SoundEngine#play and the SoundEngine instance.
    // Not a thread-safe way to do it but this code is only called from the render thread as far as I can tell.
    @Unique private static SoundInstance soundInstanceCapture;
    @Unique private static SoundEngine thisCapture;

    @Inject(
        method = "lambda$play$8(Lnet/minecraft/client/sounds/AudioStream;Lcom/mojang/blaze3d/audio/Channel;)V",
        at = @At( "HEAD" ),
        cancellable = true
    )
    private static void onStreamingSourcePlay( AudioStream audioStream, Channel channel, CallbackInfo ci )
    {
        if( CustomClientEvents.PLAY_STREAMING_AUDIO_EVENT.invoker().onPlayStreamingAudio( thisCapture, soundInstanceCapture, channel ) )
        {
            ci.cancel();
        }
    }

    @Inject(
        method = "play",
        at = @At( "HEAD" )
    )
    void onPlay( SoundInstance soundInstance, CallbackInfo ci )
    {
        soundInstanceCapture = soundInstance;
        thisCapture = (SoundEngine) (Object) this;
    }
}
