/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class Generator<T>
{
    private static final AtomicInteger METHOD_ID = new AtomicInteger();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[] { Type.getInternalName( LuaException.class ) };

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName( MethodResult.class );
    private static final String DESC_METHOD_RESULT = Type.getDescriptor( MethodResult.class );

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;

    private final Function<T, T> wrap;

    private final LoadingCache<Class<?>, List<NamedMethod<T>>> classCache = CacheBuilder
        .newBuilder()
        .build( CacheLoader.from( this::build ) );

    private final LoadingCache<Method, Optional<T>> methodCache = CacheBuilder
        .newBuilder()
        .build( CacheLoader.from( this::build ) );

    Generator( Class<T> base, List<Class<?>> context, Function<T, T> wrap )
    {
        this.base = base;
        this.context = context;
        this.interfaces = new String[] { Type.getInternalName( base ) };
        this.wrap = wrap;

        StringBuilder methodDesc = new StringBuilder().append( "(Ljava/lang/Object;" );
        for( Class<?> klass : context ) methodDesc.append( Type.getDescriptor( klass ) );
        methodDesc.append( "[Ljava/lang/Object;)" ).append( DESC_METHOD_RESULT );
        this.methodDesc = methodDesc.toString();
    }

    @Nonnull
    public List<NamedMethod<T>> getMethods( @Nonnull Class<?> klass )
    {
        try
        {
            return classCache.get( klass );
        }
        catch( ExecutionException e )
        {
            ComputerCraft.log.error( "Error getting methods for {}.", klass.getName(), e.getCause() );
            return Collections.emptyList();
        }
    }

    @Nonnull
    private List<NamedMethod<T>> build( Class<?> klass )
    {
        ArrayList<NamedMethod<T>> methods = null;
        for( Method method : klass.getMethods() )
        {
            LuaFunction annotation = method.getAnnotation( LuaFunction.class );
            if( annotation == null ) continue;

            T instance = methodCache.getUnchecked( method ).orElse( null );
            if( instance == null ) continue;

            if( methods == null ) methods = new ArrayList<>();

            if( annotation.mainThread() ) instance = wrap.apply( instance );

            String[] names = annotation.value();
            boolean isSimple = method.getReturnType() != MethodResult.class && !annotation.mainThread();
            if( names.length == 0 )
            {
                methods.add( new NamedMethod<>( method.getName(), instance, isSimple ) );
            }
            else
            {
                for( String name : names )
                {
                    methods.add( new NamedMethod<>( name, instance, isSimple ) );
                }
            }
        }

        if( methods == null ) return Collections.emptyList();
        methods.trimToSize();
        return Collections.unmodifiableList( methods );
    }

    @Nonnull
    private Optional<T> build( Method method )
    {
        String name = method.getDeclaringClass().getName() + "." + method.getName();
        int modifiers = method.getModifiers();
        if( !Modifier.isFinal( modifiers ) )
        {
            ComputerCraft.log.warn( "Lua Method {} should be final.", name );
        }

        if( Modifier.isStatic( modifiers ) || !Modifier.isPublic( modifiers ) )
        {
            ComputerCraft.log.error( "Lua Method {} should be a public instance method.", name );
            return Optional.empty();
        }

        if( !Modifier.isPublic( method.getDeclaringClass().getModifiers() ) )
        {
            ComputerCraft.log.error( "Lua Method {} should be on a public class.", name );
            return Optional.empty();
        }

        ComputerCraft.log.debug( "Generating method wrapper for {}.", name );

        Class<?>[] exceptions = method.getExceptionTypes();
        for( Class<?> exception : exceptions )
        {
            if( exception != LuaException.class )
            {
                ComputerCraft.log.error( "Lua Method {} cannot throw {}.", name, exception.getName() );
                return Optional.empty();
            }
        }

        try
        {
            String className = method.getDeclaringClass().getName() + "$cc$" + method.getName() + METHOD_ID.getAndIncrement();
            byte[] bytes = generate( className, method );
            if( bytes == null ) return Optional.empty();

            Class<?> klass = DeclaringClassLoader.INSTANCE.define( className, bytes, method.getDeclaringClass().getProtectionDomain() );
            return Optional.of( klass.asSubclass( base ).newInstance() );
        }
        catch( InstantiationException | IllegalAccessException | ClassFormatError | RuntimeException e )
        {
            ComputerCraft.log.error( "Error generating wrapper for {}.", name, e );
            return Optional.empty();
        }

    }

    @Nullable
    private byte[] generate( String className, Method method )
    {
        String internalName = className.replace( ".", "/" );

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        ClassWriter cw = new ClassWriter( ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS );
        cw.visit( V1_8, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces );
        cw.visitSource( "CC generated method", null );

        { // Constructor just invokes super.
            MethodVisitor mw = cw.visitMethod( ACC_PUBLIC, "<init>", "()V", null, null );
            mw.visitCode();
            mw.visitVarInsn( ALOAD, 0 );
            mw.visitMethodInsn( INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false );
            mw.visitInsn( RETURN );
            mw.visitMaxs( 0, 0 );
            mw.visitEnd();
        }

        {
            MethodVisitor mw = cw.visitMethod( ACC_PUBLIC, METHOD_NAME, methodDesc, null, EXCEPTIONS );
            mw.visitCode();
            mw.visitVarInsn( ALOAD, 1 );
            mw.visitTypeInsn( CHECKCAST, Type.getInternalName( method.getDeclaringClass() ) );

            for( Class<?> arg : method.getParameterTypes() )
            {
                if( arg == Object[].class )
                {
                    mw.visitVarInsn( ALOAD, 2 + context.size() );
                }
                else
                {
                    int idx = context.indexOf( arg );
                    if( idx < 0 )
                    {
                        ComputerCraft.log.error( "Unknown parameter type {} for method {}.{}.",
                            arg.getName(), method.getDeclaringClass().getName(), method.getName() );
                        return null;
                    }

                    mw.visitVarInsn( ALOAD, 2 + idx );
                }
            }

            mw.visitMethodInsn( INVOKEVIRTUAL, Type.getInternalName( method.getDeclaringClass() ), method.getName(),
                Type.getMethodDescriptor( method ), false );

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            Class<?> ret = method.getReturnType();
            if( ret != MethodResult.class )
            {
                if( ret == void.class )
                {
                    mw.visitMethodInsn( INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "()" + DESC_METHOD_RESULT, false );
                }
                else if( ret.isPrimitive() )
                {
                    Class<?> boxed = Primitives.wrap( ret );
                    mw.visitMethodInsn( INVOKESTATIC, Type.getInternalName( boxed ), "valueOf", "(" + Type.getDescriptor( ret ) + ")" + Type.getDescriptor( boxed ), false );
                    mw.visitMethodInsn( INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false );
                }
                else if( ret == Object[].class )
                {
                    mw.visitMethodInsn( INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "([Ljava/lang/Object;)" + DESC_METHOD_RESULT, false );
                }
                else
                {
                    mw.visitMethodInsn( INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false );
                }
            }

            mw.visitInsn( ARETURN );

            mw.visitMaxs( 0, 0 );
            mw.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

}
