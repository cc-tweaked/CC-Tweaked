/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic method is a method belonging to a {@link GenericSource} with a known target.
 */
public class GenericMethod
{
    final Method method;
    final LuaFunction annotation;
    final Class<?> target;
    final PeripheralType peripheralType;

    private static final List<GenericSource> sources = new ArrayList<>();
    private static List<GenericMethod> cache;

    GenericMethod( Method method, LuaFunction annotation, Class<?> target, PeripheralType peripheralType )
    {
        this.method = method;
        this.annotation = annotation;
        this.target = target;
        this.peripheralType = peripheralType;
    }

    /**
     * Find all public static methods annotated with {@link LuaFunction} which belong to a {@link GenericSource}.
     *
     * @return All available generic methods.
     */
    static List<GenericMethod> all()
    {
        if( cache != null ) return cache;
        return cache = sources.stream().flatMap( GenericMethod::getMethods ).collect( Collectors.toList() );
    }

    public static synchronized void register( @Nonnull GenericSource source )
    {
        Objects.requireNonNull( source, "Source cannot be null" );

        if( cache != null )
        {
            ComputerCraft.log.warn( "Registering a generic source {} after cache has been built. This source will be ignored.", cache );
        }

        sources.add( source );
    }

    private static Stream<GenericMethod> getMethods( GenericSource source )
    {
        Class<?> klass = source.getClass();
        PeripheralType type = source instanceof GenericPeripheral ? ((GenericPeripheral) source).getType() : null;

        return Arrays.stream( klass.getDeclaredMethods() )
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

                return new GenericMethod( method, annotation, target, type );
            } )
            .filter( Objects::nonNull );
    }
}
