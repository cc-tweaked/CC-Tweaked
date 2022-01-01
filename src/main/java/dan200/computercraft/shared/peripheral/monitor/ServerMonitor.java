/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.util.TickScheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMonitor extends ServerTerminal
{
    private final TileMonitor origin;
    private int textScale = 2;
    private final AtomicBoolean resized = new AtomicBoolean( false );
    private final AtomicBoolean changed = new AtomicBoolean( false );

    public ServerMonitor( boolean colour, TileMonitor origin )
    {
        super( colour );
        this.origin = origin;
    }

    public synchronized void rebuild()
    {
        Terminal oldTerm = getTerminal();
        int oldWidth = oldTerm == null ? -1 : oldTerm.getWidth();
        int oldHeight = oldTerm == null ? -1 : oldTerm.getHeight();

        double textScale = this.textScale * 0.5;
        int termWidth = (int) Math.max(
            Math.round( (origin.getWidth() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 6.0 * TileMonitor.RENDER_PIXEL_SCALE) ),
            1.0
        );
        int termHeight = (int) Math.max(
            Math.round( (origin.getHeight() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 9.0 * TileMonitor.RENDER_PIXEL_SCALE) ),
            1.0
        );

        resize( termWidth, termHeight );
        if( oldWidth != termWidth || oldHeight != termHeight )
        {
            getTerminal().clear();
            resized.set( true );
            markChanged();
        }
    }

    @Override
    protected void markTerminalChanged()
    {
        super.markTerminalChanged();
        markChanged();
    }

    private void markChanged()
    {
        if( !changed.getAndSet( true ) ) TickScheduler.schedule( origin );
    }

    protected void clearChanged()
    {
        changed.set( false );
    }

    public int getTextScale()
    {
        return textScale;
    }

    public synchronized void setTextScale( int textScale )
    {
        if( this.textScale == textScale ) return;
        this.textScale = textScale;
        rebuild();
    }

    public boolean pollResized()
    {
        return resized.getAndSet( false );
    }

    public boolean pollTerminalChanged()
    {
        update();
        return hasTerminalChanged();
    }
}
