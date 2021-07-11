/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.NbtCompound;

public class ServerTerminal implements ITerminal
{
    private final boolean colour;
    private final AtomicBoolean terminalChanged = new AtomicBoolean( false );
    private Terminal terminal;
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

    protected void markTerminalChanged()
    {
        terminalChanged.set( true );
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

    public void writeDescription( NbtCompound nbt )
    {
        nbt.putBoolean( "colour", colour );
        if( terminal != null )
        {
            NbtCompound terminal = new NbtCompound();
            terminal.putInt( "term_width", this.terminal.getWidth() );
            terminal.putInt( "term_height", this.terminal.getHeight() );
            this.terminal.writeToNBT( terminal );
            nbt.put( "terminal", terminal );
        }
    }
}
