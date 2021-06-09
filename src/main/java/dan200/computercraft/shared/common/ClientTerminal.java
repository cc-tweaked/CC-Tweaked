/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;
import net.minecraft.nbt.CompoundTag;

public class ClientTerminal implements ITerminal
{
    private boolean m_colour;
    private Terminal m_terminal;
    private boolean m_terminalChanged;

    public ClientTerminal( boolean colour )
    {
        this.m_colour = colour;
        this.m_terminal = null;
        this.m_terminalChanged = false;
    }

    public boolean pollTerminalChanged()
    {
        boolean changed = this.m_terminalChanged;
        this.m_terminalChanged = false;
        return changed;
    }

    // ITerminal implementation

    @Override
    public Terminal getTerminal()
    {
        return this.m_terminal;
    }

    @Override
    public boolean isColour()
    {
        return this.m_colour;
    }

    public void read( TerminalState state )
    {
        this.m_colour = state.colour;
        if( state.hasTerminal() )
        {
            this.resizeTerminal( state.width, state.height );
            state.apply( this.m_terminal );
        }
        else
        {
            this.deleteTerminal();
        }
    }

    private void resizeTerminal( int width, int height )
    {
        if( this.m_terminal == null )
        {
            this.m_terminal = new Terminal( width, height, () -> this.m_terminalChanged = true );
            this.m_terminalChanged = true;
        }
        else
        {
            this.m_terminal.resize( width, height );
        }
    }

    private void deleteTerminal()
    {
        if( this.m_terminal != null )
        {
            this.m_terminal = null;
            this.m_terminalChanged = true;
        }
    }

    public void readDescription( CompoundTag nbt )
    {
        this.m_colour = nbt.getBoolean( "colour" );
        if( nbt.contains( "terminal" ) )
        {
            CompoundTag terminal = nbt.getCompound( "terminal" );
            this.resizeTerminal( terminal.getInt( "term_width" ), terminal.getInt( "term_height" ) );
            this.m_terminal.readFromNBT( terminal );
        }
        else
        {
            this.deleteTerminal();
        }
    }
}
