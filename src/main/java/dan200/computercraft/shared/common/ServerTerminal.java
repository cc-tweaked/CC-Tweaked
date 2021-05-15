/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import java.util.concurrent.atomic.AtomicBoolean;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.network.client.TerminalState;

import net.minecraft.nbt.CompoundTag;

public class ServerTerminal implements ITerminal {
    private final boolean m_colour;
    private final AtomicBoolean m_terminalChanged = new AtomicBoolean(false);
    private Terminal m_terminal;
    private boolean m_terminalChangedLastFrame = false;

    public ServerTerminal(boolean colour) {
        this.m_colour = colour;
        this.m_terminal = null;
    }

    public ServerTerminal(boolean colour, int terminalWidth, int terminalHeight) {
        this.m_colour = colour;
        this.m_terminal = new Terminal(terminalWidth, terminalHeight, this::markTerminalChanged);
    }

    protected void markTerminalChanged() {
        this.m_terminalChanged.set(true);
    }

    protected void resize(int width, int height) {
        if (this.m_terminal == null) {
            this.m_terminal = new Terminal(width, height, this::markTerminalChanged);
            this.markTerminalChanged();
        } else {
            this.m_terminal.resize(width, height);
        }
    }

    public void delete() {
        if (this.m_terminal != null) {
            this.m_terminal = null;
            this.markTerminalChanged();
        }
    }

    public void update() {
        this.m_terminalChangedLastFrame = this.m_terminalChanged.getAndSet(false);
    }

    public boolean hasTerminalChanged() {
        return this.m_terminalChangedLastFrame;
    }

    @Override
    public Terminal getTerminal() {
        return this.m_terminal;
    }

    @Override
    public boolean isColour() {
        return this.m_colour;
    }

    public TerminalState write() {
        return new TerminalState(this.m_colour, this.m_terminal);
    }

    public void writeDescription(CompoundTag nbt) {
        nbt.putBoolean("colour", this.m_colour);
        if (this.m_terminal != null) {
            CompoundTag terminal = new CompoundTag();
            terminal.putInt("term_width", this.m_terminal.getWidth());
            terminal.putInt("term_height", this.m_terminal.getHeight());
            this.m_terminal.writeToNBT(terminal);
            nbt.put("terminal", terminal);
        }
    }
}
