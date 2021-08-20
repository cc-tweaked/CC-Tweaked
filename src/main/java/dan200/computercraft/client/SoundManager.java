/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundManager
{
    private static final Map<UUID, MoveableSound> sounds = new HashMap<>();

    public static void playSound( UUID source, Vec3 position, SoundEvent event, float volume, float pitch )
    {
        var soundManager = Minecraft.getInstance().getSoundManager();

        MoveableSound oldSound = sounds.get( source );
        if( oldSound != null ) soundManager.stop( oldSound );

        MoveableSound newSound = new MoveableSound( event, position, volume, pitch );
        sounds.put( source, newSound );
        soundManager.play( newSound );
    }

    public static void stopSound( UUID source )
    {
        SoundInstance sound = sounds.remove( source );
        if( sound == null ) return;

        Minecraft.getInstance().getSoundManager().stop( sound );
    }

    public static void moveSound( UUID source, Vec3 position )
    {
        MoveableSound sound = sounds.get( source );
        if( sound != null ) sound.setPosition( position );
    }

    public static void reset()
    {
        sounds.clear();
    }

    private static class MoveableSound extends AbstractSoundInstance implements TickableSoundInstance
    {
        protected MoveableSound( SoundEvent sound, Vec3 position, float volume, float pitch )
        {
            super( sound, SoundSource.RECORDS );
            setPosition( position );
            this.volume = volume;
            this.pitch = pitch;
            attenuation = Attenuation.LINEAR;
        }

        void setPosition( Vec3 position )
        {
            x = (float) position.x();
            y = (float) position.y();
            z = (float) position.z();
        }

        @Override
        public boolean isStopped()
        {
            return false;
        }

        @Override
        public void tick()
        {
        }
    }
}
