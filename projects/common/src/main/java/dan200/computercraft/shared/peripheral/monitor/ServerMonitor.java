// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.monitor;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.util.TickScheduler;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMonitor {
    private final MonitorBlockEntity origin;

    private final boolean colour;
    private int textScale = 2;
    private @Nullable NetworkedTerminal terminal;
    private final AtomicBoolean resized = new AtomicBoolean(false);
    private final AtomicBoolean changed = new AtomicBoolean(false);

    ServerMonitor(boolean colour, MonitorBlockEntity origin) {
        this.colour = colour;
        this.origin = origin;
    }

    synchronized void rebuild() {
        Terminal oldTerm = getTerminal();
        var oldWidth = oldTerm == null ? -1 : oldTerm.getWidth();
        var oldHeight = oldTerm == null ? -1 : oldTerm.getHeight();

        var textScale = this.textScale * 0.5;
        var termWidth = (int) Math.max(
            (double) Math.round((origin.getWidth() - 2.0 * (MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN)) / (textScale * 6.0 * MonitorBlockEntity.RENDER_PIXEL_SCALE)),
            1.0
        );
        var termHeight = (int) Math.max(
            (double) Math.round((origin.getHeight() - 2.0 * (MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN)) / (textScale * 9.0 * MonitorBlockEntity.RENDER_PIXEL_SCALE)),
            1.0
        );

        if (terminal == null) {
            terminal = new NetworkedTerminal(termWidth, termHeight, colour, this::markChanged);
            markChanged();
        } else {
            terminal.resize(termWidth, termHeight);
        }

        if (oldWidth != termWidth || oldHeight != termHeight) {
            terminal.clear();
            resized.set(true);
            markChanged();
        }
    }

    synchronized void reset() {
        if (terminal == null) return;
        terminal = null;
        markChanged();
    }

    private void markChanged() {
        if (!changed.getAndSet(true)) TickScheduler.schedule(origin.tickToken);
    }

    int getTextScale() {
        return textScale;
    }

    synchronized void setTextScale(int textScale) {
        if (this.textScale == textScale) return;
        this.textScale = textScale;
        rebuild();
    }

    boolean pollResized() {
        return resized.getAndSet(false);
    }

    boolean pollTerminalChanged() {
        return changed.getAndSet(false);
    }

    @Nullable
    @VisibleForTesting
    public NetworkedTerminal getTerminal() {
        return terminal;
    }
}
