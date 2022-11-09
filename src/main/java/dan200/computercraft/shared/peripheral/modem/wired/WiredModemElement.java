/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetworkChange;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.HashMap;
import java.util.Map;

public abstract class WiredModemElement implements IWiredElement {
    private final IWiredNode node = ComputerCraftAPI.createWiredNodeForElement(this);
    private final Map<String, IPeripheral> remotePeripherals = new HashMap<>();

    @Override
    public IWiredNode getNode() {
        return node;
    }

    @Override
    public String getSenderID() {
        return "modem";
    }

    @Override
    public void networkChanged(IWiredNetworkChange change) {
        synchronized (remotePeripherals) {
            remotePeripherals.keySet().removeAll(change.peripheralsRemoved().keySet());
            for (var name : change.peripheralsRemoved().keySet()) {
                detachPeripheral(name);
            }

            for (var peripheral : change.peripheralsAdded().entrySet()) {
                attachPeripheral(peripheral.getKey(), peripheral.getValue());
            }
            remotePeripherals.putAll(change.peripheralsAdded());
        }
    }

    public Map<String, IPeripheral> getRemotePeripherals() {
        return remotePeripherals;
    }

    protected abstract void attachPeripheral(String name, IPeripheral peripheral);

    protected abstract void detachPeripheral(String name);
}
