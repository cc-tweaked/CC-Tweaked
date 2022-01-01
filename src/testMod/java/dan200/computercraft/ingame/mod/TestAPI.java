/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.ingame.api.ComputerState;
import dan200.computercraft.ingame.api.TestExtensionsKt;
import net.minecraft.gametest.framework.GameTestSequence;

import java.util.Optional;

/**
 * API exposed to computers to help write tests.
 *
 * Note, we extend this API within startup file of computers (see {@code cctest.lua}).
 *
 * @see TestExtensionsKt#thenComputerOk(GameTestSequence, String, String)   To check tests on the computer have passed.
 */
public class TestAPI extends ComputerState implements ILuaAPI
{
    private final IComputerSystem system;
    private String label;

    TestAPI( IComputerSystem system )
    {
        this.system = system;
    }

    @Override
    public void startup()
    {
        if( label == null ) label = system.getLabel();
        if( label == null )
        {
            label = "#" + system.getID();
            ComputerCraft.log.warn( "Computer {} has no label", label );
        }

        ComputerCraft.log.info( "Computer '{}' has turned on.", label );
        markers.clear();
        error = null;
        lookup.put( label, this );
    }

    @Override
    public void shutdown()
    {
        ComputerCraft.log.info( "Computer '{}' has shut down.", label );
        if( lookup.get( label ) == this ) lookup.remove( label );
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "test" };
    }

    @LuaFunction
    public final void fail( String message ) throws LuaException
    {
        ComputerCraft.log.error( "Computer '{}' failed with {}", label, message );
        if( markers.contains( ComputerState.DONE ) ) throw new LuaException( "Cannot call fail/ok multiple times." );
        markers.add( ComputerState.DONE );
        error = message;
        throw new LuaException( message );
    }

    @LuaFunction
    public final void ok( Optional<String> marker ) throws LuaException
    {
        String actualMarker = marker.orElse( ComputerState.DONE );
        if( markers.contains( ComputerState.DONE ) || markers.contains( actualMarker ) )
        {
            throw new LuaException( "Cannot call fail/ok multiple times." );
        }

        markers.add( actualMarker );
    }

    @LuaFunction
    public final void log( String message )
    {
        ComputerCraft.log.info( "[Computer '{}'] {}", label, message );
    }
}
