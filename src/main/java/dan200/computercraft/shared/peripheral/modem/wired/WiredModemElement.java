/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetworkChange;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.wired.WiredNode;

public abstract class WiredModemElement implements IWiredElement {
    private final IWiredNode node = new WiredNode(this);
    private final Map<String, IPeripheral> remotePeripherals = new HashMap<>();

    @Nonnull
    @Override
    public IWiredNode getNode() {
        return this.node;
    }

    @Nonnull
    @Override
    public String getSenderID() {
        return "modem";
    }

    @Override
    public void networkChanged(@Nonnull IWiredNetworkChange change) {
        synchronized (this.remotePeripherals) {
            this.remotePeripherals.keySet()
                                  .removeAll(change.peripheralsRemoved()
                                              .keySet());
            for (String name : change.peripheralsRemoved()
                                     .keySet()) {
                this.detachPeripheral(name);
            }

            for (Map.Entry<String, IPeripheral> peripheral : change.peripheralsAdded()
                                                                   .entrySet()) {
                this.attachPeripheral(peripheral.getKey(), peripheral.getValue());
            }
            this.remotePeripherals.putAll(change.peripheralsAdded());
        }
    }

    protected abstract void detachPeripheral(String name);

    protected abstract void attachPeripheral(String name, IPeripheral peripheral);

    public Map<String, IPeripheral> getRemotePeripherals() {
        return this.remotePeripherals;
    }
}
