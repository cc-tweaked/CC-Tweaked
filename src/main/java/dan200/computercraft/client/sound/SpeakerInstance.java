/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.ComputerCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/**
 * An instance of a speaker, which is either playing a {@link DfpwmStream} stream or a normal sound.
 */
public class SpeakerInstance
{
    public static final ResourceLocation DFPWM_STREAM = new ResourceLocation( ComputerCraft.MOD_ID, "speaker.dfpwm_fake_audio_should_not_be_played" );

    private DfpwmStream currentStream;
    private SpeakerSound sound;

    SpeakerInstance()
    {
    }

    public synchronized void pushAudio( ByteBuf buffer )
    {
        if( currentStream == null ) currentStream = new DfpwmStream();
        currentStream.push( buffer );
    }

    public void playAudio( Vector3d position, float volume )
    {
        SoundHandler soundManager = Minecraft.getInstance().getSoundManager();

        if( sound != null && sound.stream != currentStream )
        {
            soundManager.stop( sound );
            sound = null;
        }

        if( sound != null && !soundManager.isActive( sound ) ) sound = null;

        if( sound == null && currentStream != null )
        {
            sound = new SpeakerSound( DFPWM_STREAM, currentStream, position, volume, 1.0f );
            soundManager.play( sound );
        }
    }

    public void playSound( Vector3d position, ResourceLocation location, float volume, float pitch )
    {
        SoundHandler soundManager = Minecraft.getInstance().getSoundManager();
        currentStream = null;

        if( sound != null )
        {
            soundManager.stop( sound );
            sound = null;
        }

        sound = new SpeakerSound( location, null, position, volume, pitch );
        soundManager.play( sound );
    }

    void setPosition( Vector3d position )
    {
        if( sound != null ) sound.setPosition( position );
    }

    void stop()
    {
        if( sound != null ) Minecraft.getInstance().getSoundManager().stop( sound );

        currentStream = null;
        sound = null;
    }
}
