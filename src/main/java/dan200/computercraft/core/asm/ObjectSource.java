/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import java.util.function.BiConsumer;

/**
 * A Lua object which exposes additional methods.
 *
 * This can be used to merge multiple objects together into one. Ideally this'd be part of the API, but I'm not entirely
 * happy with the interface - something I'd like to think about first.
 */
public interface ObjectSource
{
    Iterable<Object> getExtra();

    static <T> void allMethods( Generator<T> generator, Object object, BiConsumer<Object, NamedMethod<T>> accept )
    {
        for( NamedMethod<T> method : generator.getMethods( object.getClass() ) ) accept.accept( object, method );

        if( object instanceof ObjectSource )
        {
            for( Object extra : ((ObjectSource) object).getExtra() )
            {
                for( NamedMethod<T> method : generator.getMethods( extra.getClass() ) ) accept.accept( extra, method );
            }
        }
    }
}
