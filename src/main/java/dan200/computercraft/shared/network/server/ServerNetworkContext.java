/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import net.minecraft.server.level.ServerPlayer;

/**
 * The context under which serverbound packets are evaluated.
 */
public interface ServerNetworkContext {
    /**
     * Get the player who sent this packet.
     *
     * @return The sending player.
     */
    ServerPlayer getSender();
}
