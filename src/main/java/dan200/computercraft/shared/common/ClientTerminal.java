/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;

public class ClientTerminal
{
    private boolean colour;
    private Terminal terminal;
    private boolean terminalChanged;

    public ClientTerminal( boolean colour )
    {
        this.colour = colour;
        terminal = null;
        terminalChanged = false;
    }

    public boolean pollTerminalChanged()
    {
        boolean changed = terminalChanged;
        terminalChanged = false;
        return changed;
    }

    public Terminal getTerminal()
    {
        return terminal;
    }

    public void read( TerminalState state )
    {
        colour = state.colour;
        if( state.hasTerminal() )
        {
            resizeTerminal( state.width, state.height );
            state.apply( terminal );
        }
        else
        {
            deleteTerminal();
        }
    }

    private void resizeTerminal( int width, int height )
    {
        if( terminal == null )
        {
            terminal = new Terminal( width, height, colour, () -> terminalChanged = true );
            terminalChanged = true;
        }
        else
        {
            terminal.resize( width, height );
        }
    }

    private void deleteTerminal()
    {
        if( terminal != null )
        {
            terminal = null;
            terminalChanged = true;
        }
    }
}
