/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.PacketSender;


/**
 * An object on a {@link WiredNetwork} capable of sending packets.
 * <p>
 * Unlike a regular {@link PacketSender}, this must be associated with the node you are attempting to
 * to send the packet from.
 */
public interface WiredSender extends PacketSender {
    /**
     * The node in the network representing this object.
     * <p>
     * This should be used as a proxy for the main network. One should send packets
     * and register receivers through this object.
     *
     * @return The node for this element.
     */
    WiredNode getNode();
}
