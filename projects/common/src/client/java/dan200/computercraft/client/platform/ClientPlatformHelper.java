// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
