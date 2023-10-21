// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.wired.WiredNetwork;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class WiredNetworkImpl implements WiredNetwork {
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    Set<WiredNodeImpl> nodes;
    private Map<String, IPeripheral> peripherals = new HashMap<>();

    WiredNetworkImpl(WiredNodeImpl node) {
        nodes = new HashSet<>(1);
        nodes.add(node);
    }

    private WiredNetworkImpl(Set<WiredNodeImpl> nodes) {
        this.nodes = nodes;
    }

    @Override
    public boolean connect(WiredNode nodeU, WiredNode nodeV) {
        var wiredU = checkNode(nodeU);
        var wiredV = checkNode(nodeV);
        if (nodeU == nodeV) throw new IllegalArgumentException("Cannot add a connection to oneself.");

        lock.writeLock().lock();
        try {
            if (nodes.isEmpty()) throw new IllegalStateException("Cannot add a connection to an empty network.");

            var hasU = wiredU.network == this;
            var hasV = wiredV.network == this;
            if (!hasU && !hasV) throw new IllegalArgumentException("Neither node is in the network.");

            // We're going to assimilate a node. Copy across all edges and vertices.
            if (!hasU || !hasV) {
                var other = hasU ? wiredV.network : wiredU.network;
                other.lock.writeLock().lock();
                try {
                    // Cache several properties for iterating over later
                    var otherPeripherals = other.peripherals;
                    var thisPeripherals = otherPeripherals.isEmpty() ? peripherals : new HashMap<>(peripherals);

                    var thisNodes = otherPeripherals.isEmpty() ? nodes : new ArrayList<>(nodes);
                    var otherNodes = other.nodes;

                    // Move all nodes across into this network, destroying the original nodes.
                    nodes.addAll(otherNodes);
                    for (var node : otherNodes) node.network = this;
                    other.nodes = Set.of();

                    // Move all peripherals across,
                    other.peripherals = Map.of();
                    peripherals.putAll(otherPeripherals);

                    if (!thisPeripherals.isEmpty()) {
                        WiredNetworkChangeImpl.added(thisPeripherals).broadcast(otherNodes);
                    }

                    if (!otherPeripherals.isEmpty()) {
                        WiredNetworkChangeImpl.added(otherPeripherals).broadcast(thisNodes);
                    }
                } finally {
                    other.lock.writeLock().unlock();
                }
            }

            var added = wiredU.neighbours.add(wiredV);
            if (added) wiredV.neighbours.add(wiredU);

            InvariantChecker.checkNetwork(this);
            InvariantChecker.checkNode(wiredU);
            InvariantChecker.checkNode(wiredV);

            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean disconnect(WiredNode nodeU, WiredNode nodeV) {
        var wiredU = checkNode(nodeU);
        var wiredV = checkNode(nodeV);
        if (nodeU == nodeV) throw new IllegalArgumentException("Cannot remove a connection to oneself.");

        lock.writeLock().lock();
        try {
            var hasU = wiredU.network == this;
            var hasV = wiredV.network == this;
            if (!hasU || !hasV) throw new IllegalArgumentException("One node is not in the network.");

            // If there was no connection to remove then split.
            if (!wiredU.neighbours.remove(wiredV)) return false;
            wiredV.neighbours.remove(wiredU);

            // Determine if there is still some connection from u to v.
            // Note this is an inlining of reachableNodes which short-circuits
            // if all nodes are reachable.
            Queue<WiredNodeImpl> enqueued = new ArrayDeque<>();
            var reachableU = new HashSet<WiredNodeImpl>();

            reachableU.add(wiredU);
            enqueued.add(wiredU);

            while (!enqueued.isEmpty()) {
                var node = enqueued.remove();
                for (var neighbour : node.neighbours) {
                    // If we can reach wiredV from wiredU then abort.
                    if (neighbour == wiredV) return true;

                    // Otherwise attempt to enqueue this neighbour as well.
                    if (reachableU.add(neighbour)) enqueued.add(neighbour);
                }
            }

            // Create a new network with all U-reachable nodes/edges and remove them
            // from the existing graph.
            var networkU = new WiredNetworkImpl(reachableU);
            networkU.lock.writeLock().lock();
            try {
                // Remove nodes from this network
                nodes.removeAll(reachableU);

                // Set network and transfer peripherals
                for (var node : reachableU) {
                    node.network = networkU;
                    networkU.peripherals.putAll(node.peripherals);
                    peripherals.keySet().removeAll(node.peripherals.keySet());
                }

                // Broadcast changes
                if (!peripherals.isEmpty()) WiredNetworkChangeImpl.removed(peripherals).broadcast(networkU.nodes);
                if (!networkU.peripherals.isEmpty()) {
                    WiredNetworkChangeImpl.removed(networkU.peripherals).broadcast(nodes);
                }

                InvariantChecker.checkNetwork(this);
                InvariantChecker.checkNetwork(networkU);
                InvariantChecker.checkNode(wiredU);
                InvariantChecker.checkNode(wiredV);

                return true;
            } finally {
                networkU.lock.writeLock().unlock();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(WiredNode node) {
        var wired = checkNode(node);

        lock.writeLock().lock();
        try {
            // If we're the empty graph then just abort: nodes must have _some_ network.
            if (nodes.isEmpty()) return false;
            if (nodes.size() <= 1) return false;
            if (wired.network != this) return false;

            var neighbours = wired.neighbours;

            // Remove this node and move into a separate network.
            nodes.remove(wired);
            for (var neighbour : neighbours) neighbour.neighbours.remove(wired);

            var wiredNetwork = new WiredNetworkImpl(wired);

            // If we're a leaf node in the graph (only one neighbour) then we don't need to
            // check for network splitting
            if (neighbours.size() == 1) {
                // Broadcast our simple peripheral changes
                removeSingleNode(wired, wiredNetwork);
                InvariantChecker.checkNode(wired);
                InvariantChecker.checkNetwork(wiredNetwork);
                return true;
            }

            var reachable = reachableNodes(neighbours.iterator().next());

            // If all nodes are reachable then exit.
            if (reachable.size() == nodes.size()) {
                // Broadcast our simple peripheral changes
                removeSingleNode(wired, wiredNetwork);
                InvariantChecker.checkNode(wired);
                InvariantChecker.checkNetwork(wiredNetwork);
                return true;
            }

            // A split may cause 2..neighbours.size() separate networks, so we
            // iterate through our neighbour list, generating child networks.
            neighbours.removeAll(reachable);
            var maximals = new ArrayList<WiredNetworkImpl>(neighbours.size() + 1);
            maximals.add(wiredNetwork);
            maximals.add(new WiredNetworkImpl(reachable));

            while (!neighbours.isEmpty()) {
                reachable = reachableNodes(neighbours.iterator().next());
                neighbours.removeAll(reachable);
                maximals.add(new WiredNetworkImpl(reachable));
            }

            for (var network : maximals) network.lock.writeLock().lock();

            try {
                // We special case the original node: detaching all peripherals when needed.
                wired.network = wiredNetwork;
                wired.peripherals = Map.of();

                // Ensure every network is finalised
                for (var network : maximals) {
                    for (var child : network.nodes) {
                        child.network = network;
                        network.peripherals.putAll(child.peripherals);
                    }
                }

                for (var network : maximals) InvariantChecker.checkNetwork(network);
                InvariantChecker.checkNode(wired);

                // Then broadcast network changes once all nodes are finalised
                for (var network : maximals) {
                    WiredNetworkChangeImpl.changeOf(peripherals, network.peripherals).broadcast(network.nodes);
                }
            } finally {
                for (var network : maximals) network.lock.writeLock().unlock();
            }

            nodes.clear();
            peripherals.clear();

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void updatePeripherals(WiredNode node, Map<String, IPeripheral> newPeripherals) {
        var wired = checkNode(node);
        Objects.requireNonNull(peripherals, "peripherals cannot be null");

        lock.writeLock().lock();
        try {
            if (wired.network != this) throw new IllegalStateException("Node is not on this network");

            var oldPeripherals = wired.peripherals;
            var change = WiredNetworkChangeImpl.changeOf(oldPeripherals, newPeripherals);
            if (change.isEmpty()) return;

            wired.peripherals = Map.copyOf(newPeripherals);

            // Detach the old peripherals then remove them.
            peripherals.keySet().removeAll(change.peripheralsRemoved().keySet());

            // Add the new peripherals and attach them
            peripherals.putAll(change.peripheralsAdded());

            change.broadcast(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    static void transmitPacket(WiredNodeImpl start, Packet packet, double range, boolean interdimensional) {
        Map<WiredNodeImpl, TransmitPoint> points = new HashMap<>();
        var transmitTo = new TreeSet<TransmitPoint>();

        {
            var startEntry = start.element.getLevel() != packet.sender().getLevel()
                ? new TransmitPoint(start, Double.POSITIVE_INFINITY, true)
                : new TransmitPoint(start, start.element.getPosition().distanceTo(packet.sender().getPosition()), false);
            points.put(start, startEntry);
            transmitTo.add(startEntry);
        }

        {
            TransmitPoint point;
            while ((point = transmitTo.pollFirst()) != null) {
                var world = point.node.element.getLevel();
                var position = point.node.element.getPosition();
                for (var neighbour : point.node.neighbours) {
                    var neighbourPoint = points.get(neighbour);

                    boolean newInterdimensional;
                    double newDistance;
                    if (world != neighbour.element.getLevel()) {
                        newInterdimensional = true;
                        newDistance = Double.POSITIVE_INFINITY;
                    } else {
                        newInterdimensional = false;
                        newDistance = point.distance + position.distanceTo(neighbour.element.getPosition());
                    }

                    if (neighbourPoint == null) {
                        var nextPoint = new TransmitPoint(neighbour, newDistance, newInterdimensional);
                        points.put(neighbour, nextPoint);
                        transmitTo.add(nextPoint);
                    } else if (newDistance < neighbourPoint.distance) {
                        transmitTo.remove(neighbourPoint);
                        neighbourPoint.distance = newDistance;
                        neighbourPoint.interdimensional = newInterdimensional;
                        transmitTo.add(neighbourPoint);
                    }
                }
            }
        }

        for (var point : points.values()) {
            point.node.tryTransmit(packet, point.distance, point.interdimensional, range, interdimensional);
        }
    }

    private void removeSingleNode(WiredNodeImpl wired, WiredNetworkImpl wiredNetwork) {
        wiredNetwork.lock.writeLock().lock();
        try {
            // Cache all the old nodes.
            Map<String, IPeripheral> wiredPeripherals = new HashMap<>(wired.peripherals);

            // Setup the new node's network
            // Detach the old peripherals then remove them from the old network
            wired.network = wiredNetwork;
            wired.neighbours.clear();
            wired.peripherals = Map.of();

            // Broadcast the change
            if (!peripherals.isEmpty()) WiredNetworkChangeImpl.removed(peripherals).broadcast(wired);

            // Now remove all peripherals from this network and broadcast the change.
            peripherals.keySet().removeAll(wiredPeripherals.keySet());
            if (!wiredPeripherals.isEmpty()) WiredNetworkChangeImpl.removed(wiredPeripherals).broadcast(nodes);

        } finally {
            wiredNetwork.lock.writeLock().unlock();
        }
    }

    private static class TransmitPoint implements Comparable<TransmitPoint> {
        final WiredNodeImpl node;
        double distance;
        boolean interdimensional;

        TransmitPoint(WiredNodeImpl node, double distance, boolean interdimensional) {
            this.node = node;
            this.distance = distance;
            this.interdimensional = interdimensional;
        }

        @Override
        public int compareTo(TransmitPoint o) {
            // Objects with the same distance are not the same object, so we must add an additional layer of ordering.
            return distance == o.distance
                ? Integer.compare(node.hashCode(), o.node.hashCode())
                : Double.compare(distance, o.distance);
        }
    }

    private static WiredNodeImpl checkNode(WiredNode node) {
        if (node instanceof WiredNodeImpl) {
            return (WiredNodeImpl) node;
        } else {
            throw new IllegalArgumentException("Unknown implementation of IWiredNode: " + node);
        }
    }

    private static Set<WiredNodeImpl> reachableNodes(WiredNodeImpl start) {
        Queue<WiredNodeImpl> enqueued = new ArrayDeque<>();
        var reachable = new HashSet<WiredNodeImpl>();

        reachable.add(start);
        enqueued.add(start);

        WiredNodeImpl node;
        while ((node = enqueued.poll()) != null) {
            for (var neighbour : node.neighbours) {
                // Otherwise attempt to enqueue this neighbour as well.
                if (reachable.add(neighbour)) enqueued.add(neighbour);
            }
        }

        return reachable;
    }
}
