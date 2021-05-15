/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import java.util.concurrent.atomic.AtomicBoolean;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.util.TickScheduler;

public class ServerMonitor extends ServerTerminal {
    private final TileMonitor origin;
    private final AtomicBoolean resized = new AtomicBoolean(false);
    private final AtomicBoolean changed = new AtomicBoolean(false);
    private int textScale = 2;

    public ServerMonitor(boolean colour, TileMonitor origin) {
        super(colour);
        this.origin = origin;
    }

    @Override
    protected void markTerminalChanged() {
        super.markTerminalChanged();
        this.markChanged();
    }

    private void markChanged() {
        if (!this.changed.getAndSet(true)) {
            TickScheduler.schedule(this.origin);
        }
    }

    protected void clearChanged() {
        this.changed.set(false);
    }

    public int getTextScale() {
        return this.textScale;
    }

    public synchronized void setTextScale(int textScale) {
        if (this.textScale == textScale) {
            return;
        }
        this.textScale = textScale;
        this.rebuild();
    }

    public synchronized void rebuild() {
        Terminal oldTerm = this.getTerminal();
        int oldWidth = oldTerm == null ? -1 : oldTerm.getWidth();
        int oldHeight = oldTerm == null ? -1 : oldTerm.getHeight();

        double textScale = this.textScale * 0.5;
        int termWidth =
            (int) Math.max(Math.round((this.origin.getWidth() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 6.0 * TileMonitor.RENDER_PIXEL_SCALE)),
                                       1.0);
        int termHeight =
            (int) Math.max(Math.round((this.origin.getHeight() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 9.0 * TileMonitor.RENDER_PIXEL_SCALE)),
                                        1.0);

        this.resize(termWidth, termHeight);
        if (oldWidth != termWidth || oldHeight != termHeight) {
            this.getTerminal().clear();
            this.resized.set(true);
            this.markChanged();
        }
    }

    public boolean pollResized() {
        return this.resized.getAndSet(false);
    }

    public boolean pollTerminalChanged() {
        this.update();
        return this.hasTerminalChanged();
    }
}
