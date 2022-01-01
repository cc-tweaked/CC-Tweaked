/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.IPacketSender;

import javax.annotation.Nonnull;

/**
 * An object on a {@link IWiredNetwork} capable of sending packets.
 *
 * Unlike a regular {@link IPacketSender}, this must be associated with the node you are attempting to
 * to send the packet from.
 */
public interface IWiredSender extends IPacketSender
{
    /**
     * The node in the network representing this object.
     *
     * This should be used as a proxy for the main network. One should send packets
     * and register receivers through this object.
     *
     * @return The node for this element.
     */
    @Nonnull
    IWiredNode getNode();
}
