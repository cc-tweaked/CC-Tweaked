/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nullable;

public final class CapabilityUtil
{
    private CapabilityUtil()
    {
    }

    @Nullable
    public static <T> LazyOptional<T> invalidate( @Nullable LazyOptional<T> cap )
    {
        if( cap != null ) cap.invalidate();
        return null;
    }

    public static <T> void invalidate( @Nullable LazyOptional<T>[] caps )
    {
        if( caps == null ) return;

        for( int i = 0; i < caps.length; i++ )
        {
            LazyOptional<T> cap = caps[i];
            if( cap != null ) cap.invalidate();
            caps[i] = null;
        }
    }

    @Nullable
    public static <T> T unwrap( LazyOptional<T> p, NonNullConsumer<LazyOptional<T>> invalidate )
    {
        if( !p.isPresent() ) return null;

        p.addListener( invalidate );
        return p.orElseThrow( NullPointerException::new );
    }

    @Nullable
    public static <T> T unwrapUnsafe( LazyOptional<T> p )
    {
        return !p.isPresent() ? null : p.orElseThrow( NullPointerException::new );
    }
}
