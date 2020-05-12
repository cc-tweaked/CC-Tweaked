/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

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

    public void writeDescription( NBTTagCompound nbt )
    {
        nbt.setBoolean( "colour", m_colour );
        if( m_terminal != null )
        {
            // We have a 10 byte header (2 integer positions, then blinking and current colours), followed by the
            // contents and palette.
            // Yes, this serialisation code is terrible, but we need to serialise to NBT in order to work with monitors
            // (or rather tile entity serialisation).
            final int length = 10 + (2 * m_terminal.getWidth() * m_terminal.getHeight()) + (16 * 3);
            ByteBuf buffer = Unpooled.buffer( length );
            m_terminal.write( new PacketBuffer( buffer ) );

            if( buffer.writableBytes() != 0 )
            {
                ComputerCraft.log.warn( "Should have written {} bytes, but have {} ({} remaining).", length, buffer.writerIndex(), buffer.writableBytes() );
            }

            NBTTagCompound terminal = new NBTTagCompound();
            terminal.setInteger( "term_width", m_terminal.getWidth() );
            terminal.setInteger( "term_height", m_terminal.getHeight() );
            terminal.setByteArray( "term_contents", buffer.array() );
            nbt.setTag( "terminal", terminal );
        }
    }
}
