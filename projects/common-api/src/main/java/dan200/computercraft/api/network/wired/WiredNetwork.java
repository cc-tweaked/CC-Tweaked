// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * A wired network is composed of one of more {@link WiredNode}s, a set of connections between them, and a series
 * of peripherals.
 * <p>
 * Networks from a connected graph. This means there is some path between all nodes on the network. Further more, if
 * there is some path between two nodes then they must be on the same network. {@link WiredNetwork} will automatically
 * handle the merging and splitting of networks (and thus changing of available nodes and peripherals) as connections
 * change.
 * <p>
 * This does mean one can not rely on the network remaining consistent between subsequent operations. Consequently,
 * it is generally preferred to use the methods provided by {@link WiredNode}.
 *
 * @see WiredNode#getNetwork()
 */
@ApiStatus.NonExtendable
public interface WiredNetwork {
    /**
     * Create a connection between two nodes.
     * <p>
     * This should only be used on the server thread.
     *
     * @param left  The first node to connect
     * @param right The second node to connect
     * @return {@code true} if a connection was created or {@code false} if the connection already exists.
     * @throws IllegalStateException    If neither node is on the network.
     * @throws IllegalArgumentException If {@code left} and {@code right} are equal.
     * @see WiredNode#connectTo(WiredNode)
     * @see WiredNetwork#connect(WiredNode, WiredNode)
     * @deprecated Use {@link WiredNode#connectTo(WiredNode)}
     */
    @Deprecated
    boolean connect(WiredNode left, WiredNode right);

    /**
     * Destroy a connection between this node and another.
     * <p>
     * This should only be used on the server thread.
     *
     * @param left  The first node in the connection.
     * @param right The second node in the connection.
     * @return {@code true} if a connection was destroyed or {@code false} if no connection exists.
     * @throws IllegalArgumentException If either node is not on the network.
     * @throws IllegalArgumentException If {@code left} and {@code right} are equal.
     * @see WiredNode#disconnectFrom(WiredNode)
     * @see WiredNetwork#connect(WiredNode, WiredNode)
     * @deprecated Use {@link WiredNode#disconnectFrom(WiredNode)}
     */
    @Deprecated
    boolean disconnect(WiredNode left, WiredNode right);

    /**
     * Sever all connections this node has, removing it from this network.
     * <p>
     * This should only be used on the server thread. You should only call this on nodes
     * that your network element owns.
     *
     * @param node The node to remove
     * @return Whether this node was removed from the network. One cannot remove a node from a network where it is the
     * only element.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see WiredNode#remove()
     * @deprecated Use {@link WiredNode#remove()}
     */
    @Deprecated
    boolean remove(WiredNode node);

    /**
     * Update the peripherals a node provides.
     * <p>
     * This should only be used on the server thread. You should only call this on nodes
     * that your network element owns.
     *
     * @param node        The node to attach peripherals for.
     * @param peripherals The new peripherals for this node.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see WiredNode#updatePeripherals(Map)
     * @deprecated Use {@link WiredNode#updatePeripherals(Map)}
     */
    @Deprecated
    void updatePeripherals(WiredNode node, Map<String, IPeripheral> peripherals);
}
