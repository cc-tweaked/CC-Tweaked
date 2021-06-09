/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTerminal implements ITerminal
{
    private final boolean colour;
    private final AtomicBoolean terminalChanged = new AtomicBoolean( false );
    private Terminal terminal;
    private boolean terminalChangedLastFrame = false;

    public ServerTerminal( boolean colour )
    {
        this.colour = colour;
        this.terminal = null;
    }

    public ServerTerminal( boolean colour, int terminalWidth, int terminalHeight )
    {
        this.colour = colour;
        this.terminal = new Terminal( terminalWidth, terminalHeight, this::markTerminalChanged );
    }

    protected void markTerminalChanged()
    {
        this.terminalChanged.set( true );
    }

    protected void resize( int width, int height )
    {
        if( this.terminal == null )
        {
            this.terminal = new Terminal( width, height, this::markTerminalChanged );
            this.markTerminalChanged();
        }
        else
        {
            this.terminal.resize( width, height );
        }
    }

    public void delete()
    {
        if( this.terminal != null )
        {
            this.terminal = null;
            this.markTerminalChanged();
        }
    }

    public void update()
    {
        this.terminalChangedLastFrame = this.terminalChanged.getAndSet( false );
    }

    public boolean hasTerminalChanged()
    {
        return this.terminalChangedLastFrame;
    }

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

    public TerminalState write()
    {
        return new TerminalState( this.colour, this.terminal );
    }

    public void writeDescription( CompoundTag nbt )
    {
        nbt.putBoolean( "colour", this.colour );
        if( this.terminal != null )
        {
            CompoundTag terminal = new CompoundTag();
            terminal.putInt( "term_width", this.terminal.getWidth() );
            terminal.putInt( "term_height", this.terminal.getHeight() );
            this.terminal.writeToNBT( terminal );
            nbt.put( "terminal", terminal );
        }
    }
}
