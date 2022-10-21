/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTerminal
{
    private final boolean colour;
    private @Nullable Terminal terminal;
    private final AtomicBoolean terminalChanged = new AtomicBoolean( false );
    private boolean terminalChangedLastFrame = false;

    public ServerTerminal( boolean colour )
    {
        this.colour = colour;
    }

    protected void resize( int width, int height )
    {
        if( terminal == null )
        {
            terminal = new Terminal( width, height, colour, this::markTerminalChanged );
            markTerminalChanged();
        }
        else
        {
            terminal.resize( width, height );
        }
    }

    public void delete()
    {
        if( terminal != null )
        {
            terminal = null;
            markTerminalChanged();
        }
    }

    protected void markTerminalChanged()
    {
        terminalChanged.set( true );
    }

    public void tickServer()
    {
        terminalChangedLastFrame = terminalChanged.getAndSet( false );
    }

    public boolean hasTerminalChanged()
    {
        return terminalChangedLastFrame;
    }

    public Terminal getTerminal()
    {
        return terminal;
    }

    public TerminalState write()
    {
        return new TerminalState( terminal );
    }
}
