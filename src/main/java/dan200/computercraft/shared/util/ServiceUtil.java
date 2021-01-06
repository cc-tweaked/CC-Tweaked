/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraftforge.fml.ModList;
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

    public static <T> Stream<T> loadServicesForge( Class<T> target )
    {
        Type type = Type.getType( target );
        ClassLoader loader = ComputerCraftAPI.class.getClassLoader();
        return ModList.get().getAllScanData().stream()
            .flatMap( x -> x.getAnnotations().stream() )
            .filter( x -> x.getAnnotationType().equals( AUTO_SERVICE ) )
            .filter( x -> {
                Object value = x.getAnnotationData().get( "value" );
                return value instanceof List<?> && ((List<?>) value).contains( type );
            } )
            .flatMap( x -> {
                try
                {
                    Class<?> klass = loader.loadClass( x.getClassType().getClassName() );
                    if( !target.isAssignableFrom( klass ) )
                    {
                        ComputerCraft.log.error( "{} is not a subtype of {}", x.getClassType().getClassName(), target.getName() );
                        return Stream.empty();
                    }

                    Class<? extends T> casted = klass.asSubclass( target );
                    return Stream.of( casted.newInstance() );
                }
                catch( ReflectiveOperationException e )
                {
                    ComputerCraft.log.error( "Cannot load {}", x.getClassType(), e );
                    return Stream.empty();
                }
            } );
    }
}
