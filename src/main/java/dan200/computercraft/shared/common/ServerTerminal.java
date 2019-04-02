/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import net.minecraft.nbt.NBTTagCompound;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTerminal implements ITerminal
{
    private final boolean m_colour;
    private Terminal m_terminal;
    private final AtomicBoolean m_terminalChanged = new AtomicBoolean( false );
    private boolean m_terminalChangedLastFrame = false;

    public ServerTerminal( boolean colour )
    {
        m_colour = colour;
        m_terminal = null;
    }

    public ServerTerminal( boolean colour, int terminalWidth, int terminalHeight )
    {
        m_colour = colour;
        m_terminal = new Terminal( terminalWidth, terminalHeight, this::markTerminalChanged );
    }

    protected void resize( int width, int height )
    {
        if( m_terminal == null )
        {
            m_terminal = new Terminal( width, height, this::markTerminalChanged );
            markTerminalChanged();
        }
        else
        {
            m_terminal.resize( width, height );
        }
    }

    public void delete()
    {
        if( m_terminal != null )
        {
            m_terminal = null;
            markTerminalChanged();
        }
    }

    protected void markTerminalChanged()
    {
        m_terminalChanged.set( true );
    }

    public void update()
    {
        Terminal terminal = m_terminal;
        if( terminal != null ) terminal.clearChanged();

        m_terminalChangedLastFrame = m_terminalChanged.getAndSet( false );
    }

    public boolean hasTerminalChanged()
    {
        return m_terminalChangedLastFrame;
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

    // Networking stuff

    public void writeDescription( NBTTagCompound nbt )
    {
        nbt.putBoolean( "colour", m_colour );
        if( m_terminal != null )
        {
            NBTTagCompound terminal = new NBTTagCompound();
            terminal.putInt( "term_width", m_terminal.getWidth() );
            terminal.putInt( "term_height", m_terminal.getHeight() );
            m_terminal.writeToNBT( terminal );
            nbt.put( "terminal", terminal );
        }
    }
}
