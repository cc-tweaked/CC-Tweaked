/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ServiceUtil
{
    private static final Type AUTO_SERVICE = Type.getType( "Lcom/google/auto/service/AutoService;" );

    private ServiceUtil()
    {
    }

    public static <T> Stream<T> loadServices( Class<T> target )
    {
        return StreamSupport.stream( ServiceLoader.load( target, ServiceUtil.class.getClassLoader() ).spliterator(), false );
    }
}
