/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import com.google.common.io.ByteStreams;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;

/**
 * Runs a test method in an entirely isolated {@link ClassLoader}, so you can mess around with as much of
 * {@link dan200.computercraft} as you like.
 *
 * This <strong>IS NOT</strong> a good idea, but helps us run some tests in parallel while having lots of (terrible)
 * global state.
 */
public class IsolatedRunner implements InvocationInterceptor, BeforeEachCallback, AfterEachCallback
{
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create( new Object() );

    @Override
    public void beforeEach( ExtensionContext context ) throws Exception
    {
        ClassLoader loader = context.getStore( NAMESPACE ).getOrComputeIfAbsent( IsolatedClassLoader.class );

        // Rename the global thread group to something more obvious.
        ThreadGroup group = (ThreadGroup) loader.loadClass( "dan200.computercraft.shared.util.ThreadUtils" ).getMethod( "group" ).invoke( null );
        Field field = ThreadGroup.class.getDeclaredField( "name" );
        UnsafeHacks.setField( field, group, "<" + context.getDisplayName() + ">" );
    }

    @Override
    public void afterEach( ExtensionContext context ) throws Exception
    {
        ClassLoader loader = context.getStore( NAMESPACE ).get( IsolatedClassLoader.class, IsolatedClassLoader.class );
        loader.loadClass( "dan200.computercraft.core.computer.ComputerThread" )
            .getDeclaredMethod( "stop" )
            .invoke( null );
    }


    @Override
    public void interceptTestMethod( Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext ) throws Throwable
    {
        invocation.skip();

        ClassLoader loader = extensionContext.getStore( NAMESPACE ).get( IsolatedClassLoader.class, IsolatedClassLoader.class );
        Method method = invocationContext.getExecutable();

        Class<?> ourClass = loader.loadClass( method.getDeclaringClass().getName() );
        Method ourMethod = ourClass.getDeclaredMethod( method.getName(), method.getParameterTypes() );

        try
        {
            ourMethod.invoke( ourClass.getConstructor().newInstance(), invocationContext.getArguments().toArray() );
        }
        catch( InvocationTargetException e )
        {
            throw e.getTargetException();
        }
    }

    private static class IsolatedClassLoader extends SecureClassLoader
    {
        IsolatedClassLoader()
        {
            super( IsolatedClassLoader.class.getClassLoader() );
        }

        @Override
        public Class<?> loadClass( String name, boolean resolve ) throws ClassNotFoundException
        {
            synchronized( getClassLoadingLock( name ) )
            {
                Class<?> c = findLoadedClass( name );
                if( c != null ) return c;

                if( name.startsWith( "dan200.computercraft." ) )
                {
                    CodeSource parentSource = getParent().loadClass( name ).getProtectionDomain().getCodeSource();

                    byte[] contents;
                    try( InputStream stream = getResourceAsStream( name.replace( '.', '/' ) + ".class" ) )
                    {
                        if( stream == null ) throw new ClassNotFoundException( name );
                        contents = ByteStreams.toByteArray( stream );
                    }
                    catch( IOException e )
                    {
                        throw new ClassNotFoundException( name, e );
                    }

                    return defineClass( name, contents, 0, contents.length, parentSource );
                }
            }

            return super.loadClass( name, resolve );
        }
    }
}
