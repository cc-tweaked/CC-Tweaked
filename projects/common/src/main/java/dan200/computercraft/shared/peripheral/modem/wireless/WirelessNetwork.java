// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.PacketReceiver;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WirelessNetwork implements PacketNetwork {
    private final Set<PacketReceiver> receivers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final @Nullable Runnable onEmpty;

    public WirelessNetwork() {
        onEmpty = null;
    }

    public WirelessNetwork(Runnable onEmpty) {
        this.onEmpty = onEmpty;
    }

    @Override
    public void addReceiver(PacketReceiver receiver) {
        Objects.requireNonNull(receiver, "device cannot be null");
        receivers.add(receiver);
    }

    @Override
    public void removeReceiver(PacketReceiver receiver) {
        Objects.requireNonNull(receiver, "device cannot be null");
        receivers.remove(receiver);
        if (receivers.isEmpty() && onEmpty != null) onEmpty.run();
    }

    @Override
    public void transmitSameDimension(Packet packet, double range) {
        Objects.requireNonNull(packet, "packet cannot be null");
        for (var device : receivers) PacketNetwork.tryTransmit(device, packet, range, false);
    }

    @Override
    public void transmitInterdimensional(Packet packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        for (var device : receivers) PacketNetwork.tryTransmit(device, packet, 0, true);
    }

    @Override
    public boolean isWireless() {
        return true;
    }
}
