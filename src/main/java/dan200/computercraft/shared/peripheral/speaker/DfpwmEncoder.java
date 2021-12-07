/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTable;

import java.nio.ByteBuffer;

class DfpwmEncoder
{
    private static final int PREC = 10;

    private int charge = 0; // q
    private int strength = 0; // s
    private boolean previousBit = false;

    ByteBuffer encode( LuaTable<?, ?> table, int size ) throws LuaException
    {
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
        return buffer;
    }
}
