// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.HashMap;
import java.util.Map;

public abstract class WiredModemElement implements WiredElement {
    private final WiredNode node = ComputerCraftAPI.createWiredNodeForElement(this);
    private final Map<String, IPeripheral> remotePeripherals = new HashMap<>();

    @Override
    public WiredNode getNode() {
        return node;
    }

    @Override
    public String getSenderID() {
        return "modem";
    }

    @Override
    public void networkChanged(WiredNetworkChange change) {
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
