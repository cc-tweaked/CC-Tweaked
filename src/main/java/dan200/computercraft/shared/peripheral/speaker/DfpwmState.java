/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.shared.util.PauseAwareTimer;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral.SAMPLE_RATE;

/**
 * Internal state of the DFPWM decoder and the state of playback.
 */
class DfpwmState
{
    private static final long SECOND = TimeUnit.SECONDS.toNanos( 1 );

    /**
     * The minimum size of the client's audio buffer. Once we have less than this on the client, we should send another
     * batch of audio.
     */
    private static final long CLIENT_BUFFER = (long) (SECOND * 0.5);

    private static final int PREC = 10;

    private int charge = 0; // q
    private int strength = 0; // s
    private boolean previousBit = false;

    private boolean unplayed = true;
    private long clientEndTime = PauseAwareTimer.getTime();
    private float pendingVolume = 1.0f;
    private ByteBuffer pendingAudio;

    synchronized boolean pushBuffer( LuaTable<?, ?> table, int size, @Nonnull Optional<Double> volume ) throws LuaException
    {
        if( pendingAudio != null ) return false;

        int outSize = size / 8;
        ByteBuffer buffer = ByteBuffer.allocate( outSize );

        for( int i = 0; i < outSize; i++ )
        {
            int thisByte = 0;
            for( int j = 1; j <= 8; j++ )
            {
                int level = table.getInt( i * 8 + j );
                if( level < -128 || level > 127 )
                {
                    throw new LuaException( "table item #" + (i * 8 + j) + " must be between -128 and 127" );
                }

                boolean currentBit = level > charge || (level == charge && charge == 127);

                // Identical to DfpwmStream. Not happy with this, but saves some inheritance.
                int target = currentBit ? 127 : -128;

                // q' <- q + (s * (t - q) + 128)/256
                int nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if( nextCharge == charge && nextCharge != target ) nextCharge += currentBit ? 1 : -1;

                int z = currentBit == previousBit ? (1 << PREC) - 1 : 0;

                int nextStrength = strength;
                if( strength != z ) nextStrength += currentBit == previousBit ? 1 : -1;
                if( nextStrength < 2 << (PREC - 8) ) nextStrength = 2 << (PREC - 8);

                charge = nextCharge;
                strength = nextStrength;
                previousBit = currentBit;

                thisByte = (thisByte >> 1) + (currentBit ? 128 : 0);
            }

            buffer.put( (byte) thisByte );
        }

        buffer.flip();

        pendingAudio = buffer;
        pendingVolume = Mth.clamp( volume.orElse( (double) pendingVolume ).floatValue(), 0.0f, 3.0f );
        return true;
    }

    boolean shouldSendPending( long now )
    {
        return pendingAudio != null && now >= clientEndTime - CLIENT_BUFFER;
    }

    ByteBuffer pullPending( long now )
    {
        ByteBuffer audio = pendingAudio;
        pendingAudio = null;
        // Compute when we should consider sending the next packet.
        clientEndTime = Math.max( now, clientEndTime ) + (audio.remaining() * SECOND * 8 / SAMPLE_RATE);
        unplayed = false;
        return audio;
    }

    boolean isPlaying()
    {
        return unplayed || clientEndTime >= PauseAwareTimer.getTime();
    }

    float getVolume()
    {
        return pendingVolume;
    }
}
