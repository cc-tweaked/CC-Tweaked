// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network;


/**
 * A packet network represents a collection of devices which can send and receive packets.
 *
 * @see Packet
 * @see PacketReceiver
 */
public interface PacketNetwork {
    /**
     * Add a receiver to the network.
     *
     * @param receiver The receiver to register to the network.
     */
    void addReceiver(PacketReceiver receiver);

    /**
     * Remove a receiver from the network.
     *
     * @param receiver The device to remove from the network.
     */
    void removeReceiver(PacketReceiver receiver);

    /**
     * Determine whether this network is wireless.
     *
     * @return Whether this network is wireless.
     */
    boolean isWireless();

    /**
     * Submit a packet for transmitting across the network. This will route the packet through the network, sending it
     * to all receivers within range (or any interdimensional ones).
     *
     * @param packet The packet to send.
     * @param range  The maximum distance this packet will be sent.
     * @see #transmitInterdimensional(Packet)
     * @see PacketReceiver#receiveSameDimension(Packet, double)
     */
    void transmitSameDimension(Packet packet, double range);

    /**
     * Submit a packet for transmitting across the network. This will route the packet through the network, sending it
     * to all receivers across all dimensions.
     *
     * @param packet The packet to send.
     * @see #transmitSameDimension(Packet, double)
     * @see PacketReceiver#receiveDifferentDimension(Packet)
     */
    void transmitInterdimensional(Packet packet);
}
