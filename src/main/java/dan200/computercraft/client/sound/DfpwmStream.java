/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;

class DfpwmStream implements AudioStream
{
    public static final int SAMPLE_RATE = SpeakerPeripheral.SAMPLE_RATE;

    private static final int PREC = 10;
    private static final int LPF_STRENGTH = 140;

    private static final AudioFormat MONO_16 = new AudioFormat( SAMPLE_RATE, 16, 1, true, false );

    private final Queue<ByteBuffer> buffers = new ArrayDeque<>( 2 );

    private int charge = 0; // q
    private int strength = 0; // s
    private int lowPassCharge;
    private boolean previousBit = false;

    DfpwmStream()
    {
    }

    void push( @Nonnull ByteBuf input )
    {
        int readable = input.readableBytes();
        ByteBuffer output = ByteBuffer.allocate( readable * 16 ).order( ByteOrder.nativeOrder() );

        for( int i = 0; i < readable; i++ )
        {
            byte inputByte = input.readByte();
            for( int j = 0; j < 8; j++ )
            {
                boolean currentBit = (inputByte & 1) != 0;
                int target = currentBit ? 127 : -128;

                // q' <- q + (s * (t - q) + 128)/256
                int nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if( nextCharge == charge && nextCharge != target ) nextCharge += currentBit ? 1 : -1;

                int z = currentBit == previousBit ? (1 << PREC) - 1 : 0;

                int nextStrength = strength;
                if( strength != z ) nextStrength += currentBit == previousBit ? 1 : -1;
                if( nextStrength < 2 << (PREC - 8) ) nextStrength = 2 << (PREC - 8);

                // Apply antijerk
                int chargeWithAntijerk = currentBit == previousBit
                    ? nextCharge
                    : nextCharge + charge + 1 >> 1;

                // And low pass filter: outQ <- outQ + ((expectedOutput - outQ) x 140 / 256)
                lowPassCharge += ((chargeWithAntijerk - lowPassCharge) * LPF_STRENGTH + 0x80) >> 8;

                charge = nextCharge;
                strength = nextStrength;
                previousBit = currentBit;

                // Ideally we'd generate an 8-bit audio buffer. However, as we're piggybacking on top of another
                // audio stream (which uses 16 bit audio), we need to keep in the same format.
                output.putShort( (short) ((byte) (lowPassCharge & 0xFF) << 8) );

                inputByte >>= 1;
            }
        }

        output.flip();
        synchronized( this )
        {
            buffers.add( output );
        }
    }

    @Nonnull
    @Override
    public AudioFormat getFormat()
    {
        return MONO_16;
    }

    @Nonnull
    @Override
    public synchronized ByteBuffer read( int capacity )
    {
        ByteBuffer result = BufferUtils.createByteBuffer( capacity );
        while( result.hasRemaining() )
        {
            ByteBuffer head = buffers.peek();
            if( head == null ) break;

            int toRead = Math.min( head.remaining(), result.remaining() );
            result.put( result.position(), head, head.position(), toRead );
            result.position( result.position() + toRead );
            head.position( head.position() + toRead );

            if( head.hasRemaining() ) break;
            buffers.remove();
        }

        result.flip();

        // This is naughty, but ensures we're not enqueuing empty buffers when the stream is exhausted.
        return result.remaining() == 0 ? null : result;
    }

    @Override
    public void close() throws IOException
    {
        buffers.clear();
    }

    public boolean isEmpty()
    {
        return buffers.isEmpty();
    }
}
