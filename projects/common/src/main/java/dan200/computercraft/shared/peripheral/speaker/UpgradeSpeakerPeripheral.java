// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;


/**
 * A speaker peripheral which is used on an upgrade, and so is only attached to one computer.
 */
public abstract class UpgradeSpeakerPeripheral extends SpeakerPeripheral {
    public static final String ADJECTIVE = "upgrade.computercraft.speaker.adjective";

    @Override
    public void detach(IComputerAccess computer) {
        super.detach(computer);

        // We could be in the process of shutting down the server, so we can't send packets in this case.
        var level = getPosition().level();
        if (level == null) return;
        var server = level.getServer();
        if (server == null || server.isStopped()) return;

        ServerNetworking.sendToAllPlayers(new SpeakerStopClientMessage(getSource()), server);
    }
}
