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
    private boolean colour;
    private Terminal terminal;
    private boolean terminalChanged;

    public ClientTerminal( boolean colour )
    {
        this.colour = colour;
        this.terminal = null;
        this.terminalChanged = false;
    }

    public boolean pollTerminalChanged()
    {
        boolean changed = this.terminalChanged;
        this.terminalChanged = false;
        return changed;
    }

    // ITerminal implementation

    @Override
    public Terminal getTerminal()
    {
        return this.terminal;
    }

    @Override
    public boolean isColour()
    {
        return this.colour;
    }

    public void read( TerminalState state )
    {
        this.colour = state.colour;
        if( state.hasTerminal() )
        {
            this.resizeTerminal( state.width, state.height );
            state.apply( this.terminal );
        }
        else
        {
            this.deleteTerminal();
        }
    }

    private void resizeTerminal( int width, int height )
    {
        if( this.terminal == null )
        {
            this.terminal = new Terminal( width, height, () -> this.terminalChanged = true );
            this.terminalChanged = true;
        }
        else
        {
            this.terminal.resize( width, height );
        }
    }

    private void deleteTerminal()
    {
        if( this.terminal != null )
        {
            this.terminal = null;
            this.terminalChanged = true;
        }
    }

    public void readDescription( CompoundTag nbt )
    {
        this.colour = nbt.getBoolean( "colour" );
        if( nbt.contains( "terminal" ) )
        {
            CompoundTag terminal = nbt.getCompound( "terminal" );
            this.resizeTerminal( terminal.getInt( "term_width" ), terminal.getInt( "term_height" ) );
            this.terminal.readFromNBT( terminal );
        }
        else
        {
            this.deleteTerminal();
        }
    }
}
