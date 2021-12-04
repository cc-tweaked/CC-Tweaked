/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin;

import dan200.computercraft.client.sound.SpeakerSound;
import net.minecraft.client.audio.AudioStreamManager;
import net.minecraft.client.audio.IAudioStream;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundEngine;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin( SoundEngine.class )
public class SoundEngineMixin
{
    @Redirect(
        method = "play",
        at = @At( value = "INVOKE", target = "Lnet/minecraft/client/audio/AudioStreamManager;getStream(Lnet/minecraft/util/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;" )
    )
    public CompletableFuture<IAudioStream> getStream( AudioStreamManager instance, ResourceLocation id, boolean isWrapper, ISound sound )
    {
        if( sound instanceof SpeakerSound )
        {
            IAudioStream stream = ((SpeakerSound) sound).getStream();
            if( stream != null ) return CompletableFuture.completedFuture( stream );
        }

        return instance.getStream( id, isWrapper );
    }
}
