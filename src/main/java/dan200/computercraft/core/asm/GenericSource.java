/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.asm;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A generic source of {@link LuaMethod} functions.
 *
 * Unlike conventional objects, the annotated methods should be {@code static}, with their target as the first
 * parameter.
 */
public interface GenericSource
{
    ResourceLocation id();

    class GenericMethod
    {
        final Method method;
        final LuaFunction annotation;
        final Class<?> target;

        private static List<GenericMethod> cache;

        GenericMethod( Method method, LuaFunction annotation, Class<?> target )
        {
            this.method = method;
            this.annotation = annotation;
            this.target = target;
        }

        static List<GenericMethod> all()
        {
            if( cache != null ) return cache;
            return cache = StreamSupport
                .stream( ServiceLoader.load( GenericSource.class, GenericSource.class.getClassLoader() ).spliterator(), false )
                .flatMap( x -> Arrays.stream( x.getClass().getDeclaredMethods() ) )
                .map( method -> {
                    LuaFunction annotation = method.getAnnotation( LuaFunction.class );
                    if( annotation == null ) return null;

                    if( !Modifier.isStatic( method.getModifiers() ) )
                    {
                        ComputerCraft.log.error( "GenericSource method {}.{} should be static.", method.getDeclaringClass(), method.getName() );
                        return null;
                    }

                    Type[] types = method.getGenericParameterTypes();
                    if( types.length == 0 )
                    {
                        ComputerCraft.log.error( "GenericSource method {}.{} has no parameters.", method.getDeclaringClass(), method.getName() );
                        return null;
                    }

                    Class<?> target = Reflect.getRawType( method, types[0], false );
                    if( target == null ) return null;

                    return new GenericMethod( method, annotation, target );
                } )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        }
    }
}
