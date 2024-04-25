// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.network;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.server.ServerNetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * Methods for sending packets from clients to the server.
 */
public final class ClientNetworking {
    private ClientNetworking() {
    }

    /**
     * Send a network message to the server.
     *
     * @param message The message to send.
     */
    public static void sendToServer(NetworkMessage<ServerNetworkContext> message) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(new ServerboundCustomPayloadPacket(message));
    }
}
