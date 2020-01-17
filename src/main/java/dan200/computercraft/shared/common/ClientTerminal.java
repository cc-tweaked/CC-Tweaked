/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import net.minecraft.nbt.NBTTagCompound;

public class ClientTerminal implements ITerminal
{
    private boolean m_colour;
    private Terminal m_terminal;
    private boolean m_terminalChanged;

    public ClientTerminal( boolean colour )
    {
        m_colour = colour;
        m_terminal = null;
        m_terminalChanged = false;
    }

    public boolean pollTerminalChanged()
    {
        boolean changed = m_terminalChanged;
        m_terminalChanged = false;

        Terminal terminal = m_terminal;
        if( terminal != null ) terminal.clearChanged();

        return changed;
    }

    // ITerminal implementation

    @Override
    public Terminal getTerminal()
    {
        return m_terminal;
    }

    @Override
    public boolean isColour()
    {
        return m_colour;
    }

    public void readDescription( NBTTagCompound nbt )
    {
        m_colour = nbt.getBoolean( "colour" );
        if( nbt.hasKey( "terminal" ) )
        {
            NBTTagCompound terminal = nbt.getCompoundTag( "terminal" );
            resizeTerminal( terminal.getInteger( "term_width" ), terminal.getInteger( "term_height" ) );
            m_terminal.readFromNBT( terminal );
        }
        else
        {
            deleteTerminal();
        }
    }

    private void resizeTerminal( int width, int height )
    {
        if( m_terminal == null )
        {
            m_terminal = new Terminal( width, height, () -> m_terminalChanged = true );
            m_terminalChanged = true;
        }
        else
        {
            m_terminal.resize( width, height );
        }
    }

    private void deleteTerminal()
    {
        if( m_terminal != null )
        {
            m_terminal = null;
            m_terminalChanged = true;
        }
    }
}
