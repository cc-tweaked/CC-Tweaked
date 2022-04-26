/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.computer.ComputerSide;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.ContramapMatcher.contramap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GeneratorTest
{
    @Test
    public void testBasic()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( Basic.class );
        assertThat( methods, contains(
            allOf(
                named( "go" ),
                contramap( is( true ), "non-yielding", NamedMethod::nonYielding )
            )
        ) );
    }

    @Test
    public void testIdentical()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( Basic.class );
        List<NamedMethod<LuaMethod>> methods2 = LuaMethod.GENERATOR.getMethods( Basic.class );
        assertThat( methods, sameInstance( methods2 ) );
    }

    @Test
    public void testIdenticalMethods()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( Basic.class );
        List<NamedMethod<LuaMethod>> methods2 = LuaMethod.GENERATOR.getMethods( Basic2.class );
        assertThat( methods, contains( named( "go" ) ) );
        assertThat( methods.get( 0 ).getMethod(), sameInstance( methods2.get( 0 ).getMethod() ) );
    }

    @Test
    public void testEmptyClass()
    {
        assertThat( LuaMethod.GENERATOR.getMethods( Empty.class ), is( empty() ) );
    }

    @Test
    public void testNonPublicClass()
    {
        assertThat( LuaMethod.GENERATOR.getMethods( NonPublic.class ), is( empty() ) );
    }

    @Test
    public void testNonInstance()
    {
        assertThat( LuaMethod.GENERATOR.getMethods( NonInstance.class ), is( empty() ) );
    }

    @Test
    public void testIllegalThrows()
    {
        assertThat( LuaMethod.GENERATOR.getMethods( IllegalThrows.class ), is( empty() ) );
    }

    @Test
    public void testCustomNames()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( CustomNames.class );
        assertThat( methods, contains( named( "go1" ), named( "go2" ) ) );
    }

    @Test
    public void testArgKinds()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( ArgKinds.class );
        assertThat( methods, containsInAnyOrder(
            named( "objectArg" ), named( "intArg" ), named( "optIntArg" ),
            named( "context" ), named( "arguments" )
        ) );
    }

    @Test
    public void testEnum() throws LuaException
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( EnumMethods.class );
        assertThat( methods, containsInAnyOrder( named( "getEnum" ), named( "optEnum" ) ) );

        assertThat( apply( methods, new EnumMethods(), "getEnum", "front" ), one( is( "FRONT" ) ) );
        assertThat( apply( methods, new EnumMethods(), "optEnum", "front" ), one( is( "FRONT" ) ) );
        assertThat( apply( methods, new EnumMethods(), "optEnum" ), one( is( "?" ) ) );
        assertThrows( LuaException.class, () -> apply( methods, new EnumMethods(), "getEnum", "not as side" ) );
    }

    @Test
    public void testMainThread() throws LuaException
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( MainThread.class );
        assertThat( methods, contains( allOf(
            named( "go" ),
            contramap( is( false ), "non-yielding", NamedMethod::nonYielding )
        ) ) );

        assertThat( apply( methods, new MainThread(), "go" ),
            contramap( notNullValue(), "callback", MethodResult::getCallback ) );
    }

    @Test
    public void testUnsafe()
    {
        List<NamedMethod<LuaMethod>> methods = LuaMethod.GENERATOR.getMethods( Unsafe.class );
        assertThat( methods, contains( named( "withUnsafe" ) ) );
    }

    public static class Basic
    {
        @LuaFunction
        public final void go()
        {}
    }

    public static class Basic2 extends Basic
    {
    }

    public static class Empty
    {
    }

    static class NonPublic
    {
        @LuaFunction
        public final void go()
        {}
    }

    public static class NonInstance
    {
        @LuaFunction
        public static void go()
        {}
    }

    public static class IllegalThrows
    {
        @LuaFunction
        public final void go() throws IOException
        {
            throw new IOException();
        }
    }

    public static class CustomNames
    {
        @LuaFunction( { "go1", "go2" } )
        public final void go()
        {}
    }

    public static class ArgKinds
    {
        @LuaFunction
        public final void objectArg( Object arg )
        {}

        @LuaFunction
        public final void intArg( int arg )
        {}

        @LuaFunction
        public final void optIntArg( Optional<Integer> arg )
        {}

        @LuaFunction
        public final void context( ILuaContext arg )
        {}

        @LuaFunction
        public final void arguments( IArguments arg )
        {}

        @LuaFunction
        public final void unknown( IComputerAccess arg )
        {}

        @LuaFunction
        public final void illegalMap( Map<String, Integer> arg )
        {}

        @LuaFunction
        public final void optIllegalMap( Optional<Map<String, Integer>> arg )
        {}
    }

    public static class EnumMethods
    {
        @LuaFunction
        public final String getEnum( ComputerSide side )
        {
            return side.name();
        }

        @LuaFunction
        public final String optEnum( Optional<ComputerSide> side )
        {
            return side.map( ComputerSide::name ).orElse( "?" );
        }
    }

    public static class MainThread
    {
        @LuaFunction( mainThread = true )
        public final void go()
        {}
    }

    public static class Unsafe
    {
        @LuaFunction( unsafe = true )
        public final void withUnsafe( LuaTable<?, ?> table )
        {}

        @LuaFunction
        public final void withoutUnsafe( LuaTable<?, ?> table )
        {}

        @LuaFunction( unsafe = true, mainThread = true )
        public final void invalid( LuaTable<?, ?> table )
        {}
    }

    private static <T> T find( Collection<NamedMethod<T>> methods, String name )
    {
        return methods.stream()
            .filter( x -> x.getName().equals( name ) )
            .map( NamedMethod::getMethod )
            .findAny()
            .orElseThrow( NullPointerException::new );
    }

    public static MethodResult apply( Collection<NamedMethod<LuaMethod>> methods, Object instance, String name, Object... args ) throws LuaException
    {
        return find( methods, name ).apply( instance, CONTEXT, new ObjectArguments( args ) );
    }

    public static Matcher<MethodResult> one( Matcher<Object> object )
    {
        return allOf(
            contramap( nullValue(), "callback", MethodResult::getCallback ),
            contramap( array( object ), "result", MethodResult::getResult )
        );
    }

    public static <T> Matcher<NamedMethod<T>> named( String method )
    {
        return contramap( is( method ), "name", NamedMethod::getName );
    }

    private static final ILuaContext CONTEXT = new ILuaContext()
    {
        @Override
        public long issueMainThreadTask( @Nonnull ILuaTask task )
        {
            return 0;
        }
    };
}
