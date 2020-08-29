/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.wired;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

import dan200.computercraft.api.network.IPacketReceiver;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IPeripheral;

public final class WiredNode implements IWiredNode {
    final IWiredElement element;
    final HashSet<WiredNode> neighbours = new HashSet<>();
    Map<String, IPeripheral> peripherals = Collections.emptyMap();
    volatile WiredNetwork network;
    private Set<IPacketReceiver> receivers;

    public WiredNode(IWiredElement element) {
        this.element = element;
        this.network = new WiredNetwork(this);
    }

    @Override
    public synchronized void addReceiver(@Nonnull IPacketReceiver receiver) {
        if (this.receivers == null) {
            this.receivers = new HashSet<>();
        }
        this.receivers.add(receiver);
    }

    @Override
    public synchronized void removeReceiver(@Nonnull IPacketReceiver receiver) {
        if (this.receivers != null) {
            this.receivers.remove(receiver);
        }
    }

    @Override
    public boolean isWireless() {
        return false;
    }

    @Override
    public void transmitSameDimension(@Nonnull Packet packet, double range) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (!(packet.getSender() instanceof IWiredSender) || ((IWiredSender) packet.getSender()).getNode() != this) {
            throw new IllegalArgumentException("Sender is not in the network");
        }

        this.acquireReadLock();
        try {
            WiredNetwork.transmitPacket(this, packet, range, false);
        } finally {
            this.network.lock.readLock()
                             .unlock();
        }
    }

    @Override
    public void transmitInterdimensional(@Nonnull Packet packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (!(packet.getSender() instanceof IWiredSender) || ((IWiredSender) packet.getSender()).getNode() != this) {
            throw new IllegalArgumentException("Sender is not in the network");
        }

        this.acquireReadLock();
        try {
            WiredNetwork.transmitPacket(this, packet, 0, true);
        } finally {
            this.network.lock.readLock()
                             .unlock();
        }
    }

    private void acquireReadLock() {
        WiredNetwork currentNetwork = this.network;
        while (true) {
            Lock lock = currentNetwork.lock.readLock();
            lock.lock();
            if (currentNetwork == this.network) {
                return;
            }


            lock.unlock();
        }
    }

    synchronized void tryTransmit(Packet packet, double packetDistance, boolean packetInterdimensional, double range, boolean interdimensional) {
        if (this.receivers == null) {
            return;
        }

        for (IPacketReceiver receiver : this.receivers) {
            if (!packetInterdimensional) {
                double receiveRange = Math.max(range, receiver.getRange()); // Ensure range is symmetrical
                if (interdimensional || receiver.isInterdimensional() || packetDistance < receiveRange) {
                    receiver.receiveSameDimension(packet,
                                                  packetDistance + this.element.getPosition()
                                                                               .distanceTo(receiver.getPosition()));
                }
            } else {
                if (interdimensional || receiver.isInterdimensional()) {
                    receiver.receiveDifferentDimension(packet);
                }
            }
        }
    }

    @Nonnull
    @Override
    public IWiredElement getElement() {
        return this.element;
    }

    @Nonnull
    @Override
    public IWiredNetwork getNetwork() {
        return this.network;
    }

    @Override
    public String toString() {
        return "WiredNode{@" + this.element.getPosition() + " (" + this.element.getClass()
                                                                               .getSimpleName() + ")}";
    }
}
