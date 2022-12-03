/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class WiredNetworkChangeImpl implements WiredNetworkChange {
    private static final WiredNetworkChangeImpl EMPTY = new WiredNetworkChangeImpl(Collections.emptyMap(), Collections.emptyMap());

    private final Map<String, IPeripheral> removed;
    private final Map<String, IPeripheral> added;

    private WiredNetworkChangeImpl(Map<String, IPeripheral> removed, Map<String, IPeripheral> added) {
        this.removed = removed;
        this.added = added;
    }

    static WiredNetworkChangeImpl changed(Map<String, IPeripheral> removed, Map<String, IPeripheral> added) {
        return new WiredNetworkChangeImpl(Collections.unmodifiableMap(removed), Collections.unmodifiableMap(added));
    }

    static WiredNetworkChangeImpl added(Map<String, IPeripheral> added) {
        return added.isEmpty() ? EMPTY : new WiredNetworkChangeImpl(Collections.emptyMap(), Collections.unmodifiableMap(added));
    }

    static WiredNetworkChangeImpl removed(Map<String, IPeripheral> removed) {
        return removed.isEmpty() ? EMPTY : new WiredNetworkChangeImpl(Collections.unmodifiableMap(removed), Collections.emptyMap());
    }

    static WiredNetworkChangeImpl changeOf(Map<String, IPeripheral> oldPeripherals, Map<String, IPeripheral> newPeripherals) {
        // Handle the trivial cases, where all peripherals have been added or removed.
        if (oldPeripherals.isEmpty() && newPeripherals.isEmpty()) {
            return EMPTY;
        } else if (oldPeripherals.isEmpty()) {
            return new WiredNetworkChangeImpl(Collections.emptyMap(), newPeripherals);
        } else if (newPeripherals.isEmpty()) {
            return new WiredNetworkChangeImpl(oldPeripherals, Collections.emptyMap());
        }

        Map<String, IPeripheral> added = new HashMap<>(newPeripherals);
        Map<String, IPeripheral> removed = new HashMap<>();

        for (var entry : oldPeripherals.entrySet()) {
            var oldKey = entry.getKey();
            var oldValue = entry.getValue();
            if (newPeripherals.containsKey(oldKey)) {
                var rightValue = added.get(oldKey);
                if (oldValue.equals(rightValue)) {
                    added.remove(oldKey);
                } else {
                    removed.put(oldKey, oldValue);
                }
            } else {
                removed.put(oldKey, oldValue);
            }
        }

        return changed(removed, added);
    }

    @Override
    public Map<String, IPeripheral> peripheralsAdded() {
        return added;
    }

    @Override
    public Map<String, IPeripheral> peripheralsRemoved() {
        return removed;
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }

    void broadcast(Iterable<WiredNodeImpl> nodes) {
        if (!isEmpty()) {
            for (var node : nodes) node.element.networkChanged(this);
        }
    }

    void broadcast(WiredNodeImpl node) {
        if (!isEmpty()) {
            node.element.networkChanged(this);
        }
    }
}
