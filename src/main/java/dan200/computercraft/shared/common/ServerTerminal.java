/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTerminal implements ITerminal
{
    private final boolean colour;
    private Terminal terminal;
    private final AtomicBoolean terminalChanged = new AtomicBoolean( false );
    private boolean terminalChangedLastFrame = false;

    public ServerTerminal( boolean colour )
    {
        this.colour = colour;
        terminal = null;
    }

    public ServerTerminal( boolean colour, int terminalWidth, int terminalHeight )
    {
        this.colour = colour;
        terminal = new Terminal( terminalWidth, terminalHeight, this::markTerminalChanged );
    }

    protected void resize( int width, int height )
    {
        if( terminal == null )
        {
            terminal = new Terminal( width, height, this::markTerminalChanged );
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

    public void update()
    {
        terminalChangedLastFrame = terminalChanged.getAndSet( false );
    }

    public boolean hasTerminalChanged()
    {
        return terminalChangedLastFrame;
    }

    @Override
    public Terminal getTerminal()
    {
        return terminal;
    }

    @Override
    public boolean isColour()
    {
        return colour;
    }

    public TerminalState write()
    {
        return new TerminalState( colour, terminal );
    }
}
