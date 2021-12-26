/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.events;

import com.mojang.blaze3d.audio.Channel;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;

public class CustomClientEvents
{
    public static final Event<ClientUnloadWorld> CLIENT_UNLOAD_WORLD_EVENT = EventFactory.createArrayBacked( ClientUnloadWorld.class,
        callbacks -> () -> {
            for( ClientUnloadWorld callback : callbacks )
            {
                callback.onClientUnloadWorld();
            }
        } );

    public static final Event<PlayStreamingAudio> PLAY_STREAMING_AUDIO_EVENT = EventFactory.createArrayBacked( PlayStreamingAudio.class,
        callbacks -> ( engine, soundInstance, channel ) -> {
            for( PlayStreamingAudio callback : callbacks )
            {
                if( callback.onPlayStreamingAudio( engine, soundInstance, channel ) ) return true;
            }
            return false;
        } );

    @FunctionalInterface
    public interface ClientUnloadWorld
    {
        void onClientUnloadWorld();
    }

    @FunctionalInterface
    public interface PlayStreamingAudio
    {
        boolean onPlayStreamingAudio( SoundEngine engine, SoundInstance soundInstance, Channel channel );
    }
}
