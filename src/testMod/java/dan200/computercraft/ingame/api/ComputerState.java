/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.api;

import dan200.computercraft.ingame.mod.TestAPI;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestSequence;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assertion state of a computer.
 *
 * @see TestAPI For the Lua interface for this.
 * @see TestExtensionsKt#thenComputerOk(GameTestSequence, String, String)
 */
public class ComputerState
{
    public static final String DONE = "DONE";

    protected static final Map<String, ComputerState> lookup = new ConcurrentHashMap<>();

    protected final Set<String> markers = new HashSet<>();
    protected String error;

    public boolean isDone( @Nonnull String marker )
    {
        return markers.contains( marker );
    }

    public void check( @Nonnull String marker )
    {
        if( !markers.contains( marker ) ) throw new IllegalStateException( "Not yet at " + marker );
        if( error != null ) throw new GameTestAssertException( error );
    }

    public static ComputerState get( String label )
    {
        return lookup.get( label );
    }
}
