/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.platform;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.server.ServerNetworkContext;

public interface ClientPlatformHelper extends dan200.computercraft.impl.client.ClientPlatformHelper {
    static ClientPlatformHelper get() {
        return (ClientPlatformHelper) dan200.computercraft.impl.client.ClientPlatformHelper.get();
    }

    /**
     * Send a network message to the server.
     *
     * @param message The message to send.
     */
    void sendToServer(NetworkMessage<ServerNetworkContext> message);
}
