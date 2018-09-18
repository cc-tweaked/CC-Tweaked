/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.computer.RunOnComputer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


@RunWith( Parameterized.class )
public class CobaltWrapperFunctionTest
{
    @Parameterized.Parameter( 0 )
    public String name;

    @Parameterized.Parameter( 1 )
    public String code;

    @Parameterized.Parameters( name = "{0}" )
    public static Collection<Object[]> parameters()
    {
        return Arrays.stream( new Object[][]{
            new Object[]{ "empty", "assert(select('#', funcs.empty()) == 0)" },
            new Object[]{ "identity", "assert(select('#', funcs.identity(1, 2, 3)) == 3)" },

            new Object[]{ "pullEvent", "os.queueEvent('test') assert(funcs.pullEvent() == 'test')" },
            new Object[]{ "pullEventTerminate", "os.queueEvent('terminate') assert(not pcall(funcs.pullEvent))" },

            new Object[]{ "pullEventRaw", "os.queueEvent('test') assert(funcs.pullEventRaw() == 'test')" },
            new Object[]{ "pullEventRawTerminate", "os.queueEvent('terminate') assert(funcs.pullEventRaw() == 'terminate')" },

            new Object[]{ "mainThread", "assert(funcs.mainThread() == 1)" },
            new Object[]{ "mainThreadMany", "for i = 1, 4 do assert(funcs.mainThread() == 1) end" }
        } ).collect( Collectors.toList() );
    }

    /**
     * Tests executing functions defined through the {@link MethodResult} API.
     */
    @Test
    public void testMethodResult() throws Exception
    {
        RunOnComputer.run( code, c -> c.addAPI( new MethodResultAPI() ) );
    }

    /**
     * Tests executing functions defined through the {@link MethodResult} API called with the
     * {@link ILuaContext} evaluator.
     */
    @Test
    public void testMethodResultEvaluate() throws Exception
    {
        RunOnComputer.run( code, c -> c.addAPI( new WrapperAPI( new MethodResultAPI() )
        {
            @Nullable
            @Override
            @Deprecated
            public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
            {
                return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
            }
        } ) );
    }

    /**
     * Tests using {@link MethodResult#then(ILuaFunction)} afterwards
     */
    @Test
    public void testMethodResultThen() throws Exception
    {
        RunOnComputer.run( code, c -> c.addAPI( new WrapperAPI( new MethodResultAPI() ) {
            @Nonnull
            @Override
            public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
            {
                return super.callMethod( context, method, arguments )
                    .then( x -> MethodResult.onMainThread( () -> MethodResult.of( x ).then( MethodResult::of ) ) )
                    .then( MethodResult::of );
            }
        } ) );
    }

    /**
     * Tests executing functions defined through the {@link ILuaContext} API.
     */
    @Test
    public void testLuaContext() throws Exception
    {
        RunOnComputer.run( code, c -> c.addAPI( new LuaContextAPI() ) );
    }

    /**
     * Tests executing functions defined through the {@link ILuaContext} API called with the
     * {@link MethodResult} evaluator.
     */
    @Test
    public void testWithLuaContext() throws Exception
    {
        RunOnComputer.run( code, c -> c.addAPI( new WrapperAPI( new LuaContextAPI() )
        {
            @Nonnull
            @Override
            @SuppressWarnings( "deprecation" )
            public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
            {
                return MethodResult.withLuaContext( c -> callMethod( c, method, arguments ) );
            }
        } ) );
    }

    private static class MethodResultAPI implements ILuaAPI
    {
        @Override
        public String[] getNames()
        {
            return new String[]{ "funcs" };
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[]{ "empty", "identity", "pullEvent", "pullEventRaw", "mainThread" };
        }

        @Nullable
        @Override
        @Deprecated
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            return callMethod( (ICallContext) context, method, arguments ).evaluate( context );
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments )
        {
            switch( method )
            {
                case 0:
                    return MethodResult.empty();
                case 1:
                    return MethodResult.of( arguments );
                case 2:
                    return MethodResult.pullEvent( "test" );
                case 3:
                    return MethodResult.pullEventRaw( "test" );
                case 4:
                    return MethodResult.onMainThread( () -> MethodResult.of( 1 ) );
                default:
                    return MethodResult.empty();
            }
        }
    }

    private static class LuaContextAPI implements ILuaAPI
    {
        @Override
        public String[] getNames()
        {
            return new String[]{ "funcs" };
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[]{ "empty", "identity", "pullEvent", "pullEventRaw", "mainThread" };
        }

        @Nullable
        @Override
        @Deprecated
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            switch( method )
            {
                case 0:
                    return null;
                case 1:
                    return arguments;
                case 2:
                    return context.pullEvent( "test" );
                case 3:
                    return context.pullEventRaw( "test" );
                case 4:
                    return context.executeMainThreadTask( () -> new Object[]{ 1 } );
                default:
                    return null;
            }
        }
    }

    public static class WrapperAPI implements ILuaAPI
    {
        private final ILuaAPI api;

        public WrapperAPI( ILuaAPI api )
        {
            this.api = api;
        }

        @Override
        public String[] getNames()
        {
            return api.getNames();
        }

        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return api.getMethodNames();
        }

        @Nullable
        @Override
        @Deprecated
        public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
        {
            return api.callMethod( context, method, arguments );
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] arguments ) throws LuaException
        {
            return api.callMethod( context, method, arguments );
        }
    }
}
