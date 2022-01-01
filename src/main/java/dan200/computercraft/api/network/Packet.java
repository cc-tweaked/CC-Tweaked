/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network;

/**
 * Represents a packet which may be sent across a {@link IPacketNetwork}.
 *
 * @param channel      The channel to send the packet along. Receiving devices should only process packets from on
 *                     channels they are listening to.
 * @param replyChannel The channel to reply on.
 * @param payload      The contents of this packet. This should be a "valid" Lua object, safe for queuing as an
 *                     event or returning from a peripheral call.
 * @param sender       The object which sent this packet.
 * @see IPacketSender
 * @see IPacketNetwork#transmitSameDimension(Packet, double)
 * @see IPacketNetwork#transmitInterdimensional(Packet)
 * @see IPacketReceiver#receiveDifferentDimension(Packet)
 * @see IPacketReceiver#receiveSameDimension(Packet, double)
 */
public record Packet(
    int channel,
    int replyChannel,
    Object payload,
    IPacketSender sender
)
{
}
