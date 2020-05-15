/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerBootstrap;
import dan200.computercraft.core.computer.ComputerSide;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MethodTest
{
    @Test
    public void testMainThread()
    {
        ComputerBootstrap.run( "assert(main_thread.go() == 123)", x -> x.addApi( new MainThread() ), 50 );
    }

    @Test
    public void testMainThreadPeripheral()
    {
        ComputerBootstrap.run( "assert(peripheral.call('top', 'go') == 123)",
            x -> x.getEnvironment().setPeripheral( ComputerSide.TOP, new MainThread() ),
            50 );
    }

    @Test
    public void testDynamic()
    {
        ComputerBootstrap.run(
            "assert(dynamic.foo() == 123, 'foo: ' .. tostring(dynamic.foo()))\n" +
                "assert(dynamic.bar() == 321, 'bar: ' .. tostring(dynamic.bar()))",
            x -> x.addApi( new Dynamic() ), 50 );
    }

    @Test
    public void testDynamicPeripheral()
    {
        ComputerBootstrap.run(
            "local dynamic = peripheral.wrap('top')\n" +
                "assert(dynamic.foo() == 123, 'foo: ' .. tostring(dynamic.foo()))\n" +
                "assert(dynamic.bar() == 321, 'bar: ' .. tostring(dynamic.bar()))",
            x -> x.getEnvironment().setPeripheral( ComputerSide.TOP, new Dynamic() ),
            50
        );
    }

    public static class MainThread implements ILuaAPI, IPeripheral
    {
        public final String thread = Thread.currentThread().getName();

        @Override
        public String[] getNames()
        {
            return new String[] { "main_thread" };
        }

        @LuaFunction( mainThread = true )
        public final int go()
        {
            assertThat( Thread.currentThread().getName(), is( thread ) );
            return 123;
        }

        @Nonnull
        @Override
        public String getType()
        {
            return "main_thread";
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other;
        }
    }

    public static class Dynamic implements IDynamicLuaObject, ILuaAPI, IDynamicPeripheral
    {
        @Nonnull
        @Override
        public String[] getMethodNames()
        {
            return new String[] { "foo" };
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull ILuaContext context, int method, @Nonnull IArguments arguments )
        {
            return MethodResult.of( 123 );
        }

        @Nonnull
        @Override
        public MethodResult callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull IArguments arguments )
        {
            return callMethod( context, method, arguments );
        }

        @LuaFunction
        public final int bar()
        {
            return 321;
        }

        @Override
        public String[] getNames()
        {
            return new String[] { "dynamic" };
        }

        @Nonnull
        @Override
        public String getType()
        {
            return "dynamic";
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other;
        }
    }
}
