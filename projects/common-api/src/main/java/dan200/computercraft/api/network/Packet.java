// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0


package dan200.computercraft.api.network;

/**
 * Represents a packet which may be sent across a {@link PacketNetwork}.
 *
 * @param channel      The channel to send the packet along. Receiving devices should only process packets from on
 *                     channels they are listening to.
 * @param replyChannel The channel to reply on.
 * @param payload      The contents of this packet. This should be a "valid" Lua object, safe for queuing as an
 *                     event or returning from a peripheral call.
 * @param sender       The object which sent this packet.
 * @see PacketSender
 * @see PacketNetwork#transmitSameDimension(Packet, double)
 * @see PacketNetwork#transmitInterdimensional(Packet)
 * @see PacketReceiver#receiveDifferentDimension(Packet)
 * @see PacketReceiver#receiveSameDimension(Packet, double)
 */
public record Packet(
    int channel,
    int replyChannel,
    Object payload,
    PacketSender sender
) {
}
