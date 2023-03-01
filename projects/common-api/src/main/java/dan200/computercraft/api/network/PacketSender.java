// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


/**
 * An object on a {@link PacketNetwork}, capable of sending packets.
 */
public interface PacketSender {
    /**
     * Get the world in which this packet sender exists.
     *
     * @return The sender's world.
     */
    Level getLevel();

    /**
     * Get the position in the world at which this sender exists.
     *
     * @return The sender's position.
     */
    Vec3 getPosition();

    /**
     * Get some sort of identification string for this sender. This does not strictly need to be unique, but you
     * should be able to extract some identifiable information from it.
     *
     * @return This device's id.
     */
    String getSenderID();
}
