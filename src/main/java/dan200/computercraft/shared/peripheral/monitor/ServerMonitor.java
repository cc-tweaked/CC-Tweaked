/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;

public class ServerMonitor extends ServerTerminal
{
    private final TileMonitor origin;
    private int textScale = 2;
    private boolean resized;

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
            resized = true;
        }
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

    public synchronized boolean pollResized()
    {
        if( resized )
        {
            resized = false;
            return true;
        }

        return false;
    }
}
