/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteBuffer;

/**
 * An instance of a speaker, which is either playing a {@link DfpwmStream} stream or a normal sound.
 */
public class SpeakerInstance
{
    private static final int BUFFER_SIZE = 1024;
    private static final int BUFFER_COUNT = 8;

    private DfpwmStream currentStream;
    private SpeakerSound sound;
    private final IntBuffer buffers;
    private final int source;
    private boolean playing = false;
    public final float maxDistance = 32;

    SpeakerInstance()
    {
        buffers = BufferUtils.createIntBuffer( BUFFER_COUNT );
        AL10.alGenBuffers( buffers );
        ShortBuffer[] bufferData = new ShortBuffer[BUFFER_COUNT];
        for( int i = 0; i < BUFFER_COUNT; i++ )
        {
            bufferData[i] = BufferUtils.createShortBuffer( BUFFER_SIZE );
            AL10.alBufferData( buffers.get( i ), AL10.AL_FORMAT_MONO16, bufferData[i], 48000 );
        }

        source = AL10.alGenSources();
        AL10.alSourcef( source, AL10.AL_ROLLOFF_FACTOR, (24F * 0.25F) / maxDistance );

        //fun stuff
        AL10.alSourcef( source, AL10.AL_GAIN, 1f );
        AL10.alSourcei( source, AL10.AL_LOOPING, AL10.AL_FALSE );

        AL10.alSourceQueueBuffers( source, buffers );

        //Trigger the source to play its sound
        AL10.alSourcePlay( source );
    }

    public synchronized void pushAudio( ByteBuf buffer )
    {
        if( currentStream == null ) currentStream = new DfpwmStream();
        currentStream.push( buffer );
    }

    public void playAudio( Vector3d position, float volume )
    {
        if( currentStream != null )
        {
            AL10.alSource3f( source, AL10.AL_POSITION, (float)position.x + 0.5f, (float)position.y + 0.5f, (float)position.z + 0.5f );
            AL10.alSourcef( source, AL10.AL_GAIN, volume );
            playing = true;
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

    void update()
    {
        if( AL10.alGetSourcei( source, AL10.AL_SOURCE_STATE ) != AL10.AL_PLAYING )
        {
            AL10.alSourcePlay( source );
            return;
        }
        int buffersProcessed = AL10.alGetSourcei( source, AL10.AL_BUFFERS_PROCESSED );
        while( buffersProcessed-- > 0 )
        {
            int buffer = AL10.alSourceUnqueueBuffers( source );
            ShortBuffer stream;
            if( playing && currentStream != null )
            {
                ByteBuffer buf = currentStream.read( BUFFER_SIZE );
                if( buf.limit() < BUFFER_SIZE )
                {
                    playing = false;
                    currentStream = null;
                }
                stream = buf.asShortBuffer();
            }
            else
            {
                stream = BufferUtils.createShortBuffer( BUFFER_SIZE );
            }
            AL10.alBufferData( buffer, AL10.AL_FORMAT_MONO16, stream, 48000 );
            AL10.alSourceQueueBuffers( source, buffer );
        }
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
