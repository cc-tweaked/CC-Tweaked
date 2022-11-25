/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Map;

/**
 * Wired nodes act as a layer between {@link WiredElement}s and {@link WiredNetwork}s.
 * <p>
 * Firstly, a node acts as a packet network, capable of sending and receiving modem messages to connected nodes. These
 * methods may be safely used on any thread.
 * <p>
 * When sending a packet, the system will attempt to find the shortest path between the two nodes based on their
 * element's position. Note that packet senders and receivers can have different locations from their associated
 * element: the distance between the two will be added to the total packet's distance.
 * <p>
 * Wired nodes also provide several convenience methods for interacting with a wired network. These should only ever
 * be used on the main server thread.
 */
public interface WiredNode extends PacketNetwork {
    /**
     * The associated element for this network node.
     *
     * @return This node's element.
     */
    WiredElement getElement();

    /**
     * The network this node is currently connected to. Note that this may change
     * after any network operation, so it should not be cached.
     * <p>
     * This should only be used on the server thread.
     *
     * @return This node's network.
     */
    WiredNetwork getNetwork();

    /**
     * Create a connection from this node to another.
     * <p>
     * This should only be used on the server thread.
     *
     * @param node The other node to connect to.
     * @return {@code true} if a connection was created or {@code false} if the connection already exists.
     * @see WiredNetwork#connect(WiredNode, WiredNode)
     * @see WiredNode#disconnectFrom(WiredNode)
     */
    default boolean connectTo(WiredNode node) {
        return getNetwork().connect(this, node);
    }

    /**
     * Destroy a connection between this node and another.
     * <p>
     * This should only be used on the server thread.
     *
     * @param node The other node to disconnect from.
     * @return {@code true} if a connection was destroyed or {@code false} if no connection exists.
     * @throws IllegalArgumentException If {@code node} is not on the same network.
     * @see WiredNetwork#disconnect(WiredNode, WiredNode)
     * @see WiredNode#connectTo(WiredNode)
     */
    default boolean disconnectFrom(WiredNode node) {
        return getNetwork().disconnect(this, node);
    }

    /**
     * Sever all connections this node has, removing it from this network.
     * <p>
     * This should only be used on the server thread. You should only call this on nodes
     * that your network element owns.
     *
     * @return Whether this node was removed from the network. One cannot remove a node from a network where it is the
     * only element.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see WiredNetwork#remove(WiredNode)
     */
    default boolean remove() {
        return getNetwork().remove(this);
    }

    /**
     * Mark this node's peripherals as having changed.
     * <p>
     * This should only be used on the server thread. You should only call this on nodes
     * that your network element owns.
     *
     * @param peripherals The new peripherals for this node.
     * @see WiredNetwork#updatePeripherals(WiredNode, Map)
     */
    default void updatePeripherals(Map<String, IPeripheral> peripherals) {
        getNetwork().updatePeripherals(this, peripherals);
    }
}
