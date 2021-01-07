/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import dan200.computercraft.shared.util.ServiceUtil;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic source of {@link LuaMethod} functions. This allows for injecting methods onto objects you do not own.
 *
 * Unlike conventional Lua objects, the annotated methods should be {@code static}, with their target as the first
 * parameter.
 *
 * This is used by the generic peripheral system ({@link GenericPeripheralProvider}) to provide methods for arbitrary
 * tile entities. Eventually this'll be be exposed in the public API. Until it is stabilised, it will remain in this
 * package - do not use it in external mods!
 */
public interface GenericSource
{
    /**
     * A unique identifier for this generic source. This may be used in the future to allow disabling specific sources.
     *
     * @return This source's identifier.
     */
    @Nonnull
    ResourceLocation id();

    /**
     * Register a stream of generic sources.
     *
     * @param sources The source of generic methods.
     * @see ServiceUtil For ways to load this. Sadly {@link java.util.ServiceLoader} is broken under Forge, but we don't
     * want to add a hard-dep on Forge within core either.
     */
    static void setup( Supplier<Stream<GenericSource>> sources )
    {
        GenericMethod.sources = sources;
    }

    /**
     * A generic method is a method belonging to a {@link GenericSource} with a known target.
     */
    class GenericMethod
    {
        final Method method;
        final LuaFunction annotation;
        final Class<?> target;

        static Supplier<Stream<GenericSource>> sources;
        private static List<GenericMethod> cache;

        GenericMethod( Method method, LuaFunction annotation, Class<?> target )
        {
            this.method = method;
            this.annotation = annotation;
            this.target = target;
        }

        /**
         * Find all public static methods annotated with {@link LuaFunction} which belong to a {@link GenericSource}.
         *
         * @return All available generic methods.
         */
        static List<GenericMethod> all()
        {
            if( cache != null ) return cache;
            if( sources == null )
            {
                ComputerCraft.log.warn( "Getting GenericMethods without a provider" );
                return cache = Collections.emptyList();
            }

            return cache = sources.get()
                .flatMap( x -> Arrays.stream( x.getClass().getDeclaredMethods() ) )
                .map( method ->
                {
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
