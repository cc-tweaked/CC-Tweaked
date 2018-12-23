/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtil
{
    public static Class<?> getOptionalClass( String name )
    {
        try
        {
            return Class.forName( name );
        }
        catch( Exception e )
        {
            // Ignore
        }
        return null;
    }

    public static Class<?> getOptionalInnerClass( Class<?> enclosingClass, String name )
    {
        if( enclosingClass != null )
        {
            try
            {
                Class<?>[] declaredClasses = enclosingClass.getDeclaredClasses();
                if( declaredClasses != null )
                {
                    for( Class<?> declaredClass : declaredClasses )
                    {
                        if( declaredClass.getSimpleName().equals( name ) )
                        {
                            return declaredClass;
                        }
                    }
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static Method getOptionalMethod( Class<?> clazz, String name, Class<?>[] arguments )
    {
        if( clazz != null )
        {
            try
            {
                return clazz.getDeclaredMethod( name, arguments );
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static <T> Constructor<T> getOptionalConstructor( Class<T> clazz, Class<?>[] arguments )
    {
        if( clazz != null )
        {
            try
            {
                return clazz.getConstructor( arguments );
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static Field getOptionalField( Class<?> clazz, String name )
    {
        if( clazz != null )
        {
            try
            {
                Field field = clazz.getDeclaredField( name );
                if( field != null )
                {
                    try
                    {
                        field.setAccessible( true );
                    }
                    catch( Exception ignored )
                    {
                    }
                }
                return field;
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static <T> T safeNew( Constructor<T> constructor, Object[] arguments, Class<T> resultClass )
    {
        if( constructor != null )
        {
            try
            {
                T result = constructor.newInstance( arguments );
                if( result != null && resultClass.isInstance( result ) )
                {
                    return result;
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static boolean safeInstanceOf( Object object, Class<?> clazz )
    {
        if( clazz != null )
        {
            return clazz.isInstance( object );
        }
        return false;
    }

    public static void safeInvoke( Method method, Object object, Object[] arguments )
    {
        if( method != null )
        {
            try
            {
                if( object == null || method.getClass().isInstance( object ) )
                {
                    method.invoke( object, arguments );
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T safeInvoke( Method method, Object object, Object[] arguments, Class<T> resultClass )
    {
        if( method != null )
        {
            try
            {
                if( (object == null && Modifier.isStatic( method.getModifiers() )) ||
                    method.getDeclaringClass().isInstance( object ) )
                {
                    Object result = method.invoke( object, arguments );
                    if( result != null && resultClass.isInstance( result ) )
                    {
                        return (T) result;
                    }
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T safeGet( Field field, Object object, Class<T> resultClass )
    {
        if( field != null )
        {
            try
            {
                if( (object == null && Modifier.isStatic( field.getModifiers() )) ||
                    field.getDeclaringClass().isInstance( object ) )
                {
                    Object result = field.get( object );
                    if( result != null && resultClass.isInstance( result ) )
                    {
                        return (T) result;
                    }
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }

    public static <T> T safeSet( Field field, Object object, T value )
    {
        if( field != null )
        {
            try
            {
                if( object == null || field.getClass().isInstance( object ) )
                {
                    field.set( object, value );
                }
            }
            catch( Exception e )
            {
                // Ignore
            }
        }
        return null;
    }
}
