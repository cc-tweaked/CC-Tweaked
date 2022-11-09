/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class ClientMonitor {
    private final TileMonitor origin;

    private @Nullable NetworkedTerminal terminal;
    private boolean terminalChanged;
    private @Nullable RenderState state;

    public ClientMonitor(TileMonitor origin) {
        this.origin = origin;
    }

    public TileMonitor getOrigin() {
        return origin;
    }

    /**
     * Get or create the current render state.
     *
     * @param create A factory to create the render state.
     * @param <T>    The current render state. This type parameter should only be inhabited by a single class.
     * @return This monitor's render state.
     */
    @SuppressWarnings("unchecked")
    public <T extends RenderState> T getRenderState(Supplier<T> create) {
        var state = this.state;
        return (T) (state != null ? state : (this.state = create.get()));
    }

    void destroy() {
        if (state != null) state.close();
        state = null;
    }

    public boolean pollTerminalChanged() {
        var changed = terminalChanged;
        terminalChanged = false;
        return changed;
    }

    public @Nullable Terminal getTerminal() {
        return terminal;
    }

    void read(TerminalState state) {
        if (state.hasTerminal()) {
            if (terminal == null) terminal = new NetworkedTerminal(state.width, state.height, state.colour);
            state.apply(terminal);
            terminalChanged = true;
        } else {
            if (terminal != null) {
                terminal = null;
                terminalChanged = true;
            }
        }
    }

    /**
     * An interface representing the current state of the monitor renderer.
     * <p>
     * This interface should only be inhabited by {@link dan200.computercraft.client.render.monitor.MonitorRenderState}:
     * it exists solely to avoid referencing client-side classes in common code.
     */
    public interface RenderState extends AutoCloseable {
        @Override
        void close();
    }
}
