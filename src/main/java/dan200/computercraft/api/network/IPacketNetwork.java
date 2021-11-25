/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network;

import javax.annotation.Nonnull;

/**
 * A packet network represents a collection of devices which can send and receive packets.
 *
 * @see Packet
 * @see IPacketReceiver
 */
public interface IPacketNetwork
{
    /**
     * Add a receiver to the network.
     *
     * @param receiver The receiver to register to the network.
     */
    void addReceiver( @Nonnull IPacketReceiver receiver );

    /**
     * Remove a receiver from the network.
     *
     * @param receiver The device to remove from the network.
     */
    void removeReceiver( @Nonnull IPacketReceiver receiver );

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
     * @see IPacketReceiver#receiveSameDimension(Packet, double)
     */
    void transmitSameDimension( @Nonnull Packet packet, double range );

    /**
     * Submit a packet for transmitting across the network. This will route the packet through the network, sending it
     * to all receivers across all dimensions.
     *
     * @param packet The packet to send.
     * @see #transmitSameDimension(Packet, double)
     * @see IPacketReceiver#receiveDifferentDimension(Packet)
     */
    void transmitInterdimensional( @Nonnull Packet packet );
}
