/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.api;

import dan200.computercraft.ingame.mod.TestAPI;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestSequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assertion state of a computer.
 *
 * @see TestAPI For the Lua interface for this.
 * @see TestExtensionsKt#thenComputerOk(GameTestSequence, int)
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
        if( !done ) throw new GameTestAssertException( "Not yet done" );
        if( error != null ) throw new GameTestAssertException( error );
    }

    public static ComputerState get( int id )
    {
        return lookup.get( id );
    }
}
