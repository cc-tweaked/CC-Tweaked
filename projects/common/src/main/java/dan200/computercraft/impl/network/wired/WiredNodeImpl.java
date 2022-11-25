/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.PacketReceiver;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNetwork;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.network.wired.WiredSender;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nullable;
import java.util.*;

public final class WiredNodeImpl implements WiredNode {
    private @Nullable Set<PacketReceiver> receivers;

    final WiredElement element;
    Map<String, IPeripheral> peripherals = Collections.emptyMap();

    final HashSet<WiredNodeImpl> neighbours = new HashSet<>();
    volatile WiredNetworkImpl network;

    public WiredNodeImpl(WiredElement element) {
        this.element = element;
        network = new WiredNetworkImpl(this);
    }

    @Override
    public synchronized void addReceiver(PacketReceiver receiver) {
        if (receivers == null) receivers = new HashSet<>();
        receivers.add(receiver);
    }

    @Override
    public synchronized void removeReceiver(PacketReceiver receiver) {
        if (receivers != null) receivers.remove(receiver);
    }

    synchronized void tryTransmit(Packet packet, double packetDistance, boolean packetInterdimensional, double range, boolean interdimensional) {
        if (receivers == null) return;

        for (var receiver : receivers) {
            if (!packetInterdimensional) {
                var receiveRange = Math.max(range, receiver.getRange()); // Ensure range is symmetrical
                if (interdimensional || receiver.isInterdimensional() || packetDistance < receiveRange) {
                    receiver.receiveSameDimension(packet, packetDistance + element.getPosition().distanceTo(receiver.getPosition()));
                }
            } else {
                if (interdimensional || receiver.isInterdimensional()) {
                    receiver.receiveDifferentDimension(packet);
                }
            }
        }
    }

    @Override
    public boolean isWireless() {
        return false;
    }

    @Override
    public void transmitSameDimension(Packet packet, double range) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (!(packet.sender() instanceof WiredSender) || ((WiredSender) packet.sender()).getNode() != this) {
            throw new IllegalArgumentException("Sender is not in the network");
        }

        acquireReadLock();
        try {
            WiredNetworkImpl.transmitPacket(this, packet, range, false);
        } finally {
            network.lock.readLock().unlock();
        }
    }

    @Override
    public void transmitInterdimensional(Packet packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (!(packet.sender() instanceof WiredSender) || ((WiredSender) packet.sender()).getNode() != this) {
            throw new IllegalArgumentException("Sender is not in the network");
        }

        acquireReadLock();
        try {
            WiredNetworkImpl.transmitPacket(this, packet, 0, true);
        } finally {
            network.lock.readLock().unlock();
        }
    }

    @Override
    public WiredElement getElement() {
        return element;
    }

    @Override
    public WiredNetwork getNetwork() {
        return network;
    }

    @Override
    public String toString() {
        return "WiredNode{@" + element.getPosition() + " (" + element.getClass().getSimpleName() + ")}";
    }

    @SuppressWarnings("LockNotBeforeTry")
    private void acquireReadLock() {
        var currentNetwork = network;
        while (true) {
            var lock = currentNetwork.lock.readLock();
            lock.lock();
            if (currentNetwork == network) return;


            lock.unlock();
        }
    }
}
