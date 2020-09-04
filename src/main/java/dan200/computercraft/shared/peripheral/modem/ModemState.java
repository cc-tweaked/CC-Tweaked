/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import java.util.concurrent.atomic.AtomicBoolean;

import dan200.computercraft.api.lua.LuaException;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class ModemState {
    private final Runnable onChanged;
    private final AtomicBoolean changed = new AtomicBoolean(true);
    private final IntSet channels = new IntOpenHashSet();
    private boolean open = false;

    public ModemState() {
        this.onChanged = null;
    }

    public ModemState(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public boolean pollChanged() {
        return this.changed.getAndSet(false);
    }

    public boolean isOpen() {
        return this.open;
    }

    private void setOpen(boolean open) {
        if (this.open == open) {
            return;
        }
        this.open = open;
        if (!this.changed.getAndSet(true) && this.onChanged != null) {
            this.onChanged.run();
        }
    }

    public boolean isOpen(int channel) {
        synchronized (this.channels) {
            return this.channels.contains(channel);
        }
    }

    public void open(int channel) throws LuaException {
        synchronized (this.channels) {
            if (!this.channels.contains(channel)) {
                if (this.channels.size() >= 128) {
                    throw new LuaException("Too many open channels");
                }
                this.channels.add(channel);
                this.setOpen(true);
            }
        }
    }

    public void close(int channel) {
        synchronized (this.channels) {
            this.channels.remove(channel);
            if (this.channels.isEmpty()) {
                this.setOpen(false);
            }
        }
    }

    public void closeAll() {
        synchronized (this.channels) {
            this.channels.clear();
            this.setOpen(false);
        }
    }
}
