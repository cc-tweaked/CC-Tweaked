/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundManager
{
    private static final Map<UUID, MoveableSound> sounds = new HashMap<>();

    public static void playSound( UUID source, Vec3d position, Identifier event, float volume, float pitch )
    {
        var soundManager = MinecraftClient.getInstance().getSoundManager();

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

        MinecraftClient.getInstance().getSoundManager().stop( sound );
    }

    public static void moveSound( UUID source, Vec3d position )
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
        protected MoveableSound( Identifier sound, Vec3d position, float volume, float pitch )
        {
            super( sound, SoundCategory.RECORDS );
            setPosition( position );
            this.volume = volume;
            this.pitch = pitch;
            attenuationType = SoundInstance.AttenuationType.LINEAR;
        }

        void setPosition( Vec3d position )
        {
            x = (float) position.getX();
            y = (float) position.getY();
            z = (float) position.getZ();
        }

        @Override
        public boolean isDone()
        {
            return false;
        }

        @Override
        public void tick()
        {
        }
    }
}
