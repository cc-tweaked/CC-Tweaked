/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.api;

import dan200.computercraft.ingame.mod.TestAPI;
import kotlin.coroutines.Continuation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assertion state of a computer.
 *
 * @see TestAPI For the Lua interface for this.
 * @see TestExtensionsKt#checkComputerOk(TestContext, int, Continuation)
 */
public class ComputerState
{
    protected static final Map<Integer, ComputerState> lookup = new ConcurrentHashMap<>();

    protected boolean done;
    protected String error;

    public boolean isDone()
    {
        return done;
    }

    public void check()
    {
        if( !done ) throw new IllegalStateException( "Not yet done" );
        if( error != null ) throw new AssertionError( error );
    }

    public static ComputerState get( int id )
    {
        return lookup.get( id );
    }
}
