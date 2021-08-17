/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.ingame.api.ComputerState;
import dan200.computercraft.ingame.api.TestContext;
import dan200.computercraft.ingame.api.TestExtensionsKt;
import kotlin.coroutines.Continuation;

/**
 * API exposed to computers to help write tests.
 *
 * Note, we extend this API within startup file of computers (see {@code cctest.lua}).
 *
 * @see TestExtensionsKt#checkComputerOk(TestContext, int, Continuation) To check tests on the computer have passed.
 */
public class TestAPI extends ComputerState implements ILuaAPI
{
    private final int id;

    TestAPI( IComputerSystem system )
    {
        id = system.getID();
    }

    @Override
    public void startup()
    {
        done = false;
        error = null;
        lookup.put( id, this );
    }

    @Override
    public void shutdown()
    {
        if( lookup.get( id ) == this ) lookup.remove( id );
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "test" };
    }

    @LuaFunction
    public final void fail( String message ) throws LuaException
    {
        if( done ) throw new LuaException( "Cannot call fail/ok multiple times." );
        done = true;
        error = message;
        throw new LuaException( message );
    }

    @LuaFunction
    public final void ok() throws LuaException
    {
        if( done ) throw new LuaException( "Cannot call fail/ok multiple times." );
        done = true;
    }
}
