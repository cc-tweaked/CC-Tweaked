/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps speakers source IDs to a {@link SpeakerInstance}.
 */
@Mod.EventBusSubscriber( Dist.CLIENT )
public class SpeakerManager
{
    private static final Map<UUID, SpeakerInstance> sounds = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void playStreaming( PlayStreamingSourceEvent event )
    {
        if( !(event.getSound() instanceof SpeakerSound) ) return;
        SpeakerSound sound = (SpeakerSound) event.getSound();
        if( sound.stream == null ) return;

        event.getSource().attachBufferStream( sound.stream );
        event.getSource().play();

        sound.source = event.getSource();
        sound.executor = event.getManager().executor;
    }

    public static SpeakerInstance getSound( UUID source )
    {
        return sounds.computeIfAbsent( source, x -> new SpeakerInstance() );
    }

    public static void stopSound( UUID source )
    {
        SpeakerInstance sound = sounds.remove( source );
        if( sound != null ) sound.stop();
    }

    public static void moveSound( UUID source, SpeakerPosition position )
    {
        SpeakerInstance sound = sounds.get( source );
        if( sound != null ) sound.setPosition( position );
    }

    public static void reset()
    {
        sounds.clear();
    }
}
