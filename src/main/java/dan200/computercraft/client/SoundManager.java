/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundManager
{
    private static final Map<UUID, MoveableSound> sounds = new HashMap<>();

    public static void playSound( UUID source, Vector3d position, ResourceLocation event, float volume, float pitch )
    {
        SoundHandler soundManager = Minecraft.getInstance().getSoundManager();

        MoveableSound oldSound = sounds.get( source );
        if( oldSound != null ) soundManager.stop( oldSound );

        MoveableSound newSound = new MoveableSound( event, position, volume, pitch );
        sounds.put( source, newSound );
        soundManager.play( newSound );
    }

    public static void stopSound( UUID source )
    {
        ISound sound = sounds.remove( source );
        if( sound == null ) return;

        Minecraft.getInstance().getSoundManager().stop( sound );
    }

    public static void moveSound( UUID source, Vector3d position )
    {
        MoveableSound sound = sounds.get( source );
        if( sound != null ) sound.setPosition( position );
    }

    public static void reset()
    {
        sounds.clear();
    }

    private static class MoveableSound extends LocatableSound implements ITickableSound
    {
        protected MoveableSound( ResourceLocation sound, Vector3d position, float volume, float pitch )
        {
            super( sound, SoundCategory.RECORDS );
            setPosition( position );
            this.volume = volume;
            this.pitch = pitch;
            attenuation = ISound.AttenuationType.LINEAR;
        }

        void setPosition( Vector3d position )
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
