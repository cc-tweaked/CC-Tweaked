// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


/**
 * An object on an {@link PacketNetwork}, capable of receiving packets.
 */
public interface PacketReceiver {
    /**
     * Get the world in which this packet receiver exists.
     *
     * @return The receivers's world.
     */
    Level getLevel();

    /**
     * Get the position in the world at which this receiver exists.
     *
     * @return The receiver's position.
     */
    Vec3 getPosition();

    /**
     * Get the maximum distance this receiver can send and receive messages.
     * <p>
     * When determining whether a receiver can receive a message, the largest distance of the packet and receiver is
     * used - ensuring it is within range. If the packet or receiver is inter-dimensional, then the packet will always
     * be received.
     *
     * @return The maximum distance this device can send and receive messages.
     * @see #isInterdimensional()
     * @see #receiveSameDimension(Packet packet, double)
     * @see PacketNetwork#transmitInterdimensional(Packet)
     */
    double getRange();

    /**
     * Determine whether this receiver can receive packets from other dimensions.
     * <p>
     * A device will receive an inter-dimensional packet if either it or the sending device is inter-dimensional.
     *
     * @return Whether this receiver receives packets from other dimensions.
     * @see #getRange()
     * @see #receiveDifferentDimension(Packet)
     * @see PacketNetwork#transmitInterdimensional(Packet)
     */
    boolean isInterdimensional();

    /**
     * Receive a network packet from the same dimension.
     *
     * @param packet   The packet to receive. Generally you should check that you are listening on the given channel and,
     *                 if so, queue the appropriate modem event.
     * @param distance The distance this packet has travelled from the source.
     * @see Packet
     * @see #getRange()
     * @see PacketNetwork#transmitSameDimension(Packet, double)
     * @see PacketNetwork#transmitInterdimensional(Packet)
     */
    void receiveSameDimension(Packet packet, double distance);

    /**
     * Receive a network packet from a different dimension.
     *
     * @param packet The packet to receive. Generally you should check that you are listening on the given channel and,
     *               if so, queue the appropriate modem event.
     * @see Packet
     * @see PacketNetwork#transmitInterdimensional(Packet)
     * @see PacketNetwork#transmitSameDimension(Packet, double)
     * @see #isInterdimensional()
     */
    void receiveDifferentDimension(Packet packet);
}
