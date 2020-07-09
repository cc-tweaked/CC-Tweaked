/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;

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
        m_terminalChangedLastFrame = m_terminalChanged.getAndSet( false );
    }

    public boolean hasTerminalChanged()
    {
        return m_terminalChangedLastFrame;
    }

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

    public TerminalState write()
    {
        return new TerminalState( m_colour, m_terminal );
    }
}
