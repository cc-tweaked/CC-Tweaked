// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;


public class PocketComputerDeletedClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID instanceId;

    public PocketComputerDeletedClientMessage(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public PocketComputerDeletedClientMessage(FriendlyByteBuf buffer) {
        instanceId = buffer.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(instanceId);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePocketComputerDeleted(instanceId);
    }

    @Override
    public MessageType<PocketComputerDeletedClientMessage> type() {
        return NetworkMessages.POCKET_COMPUTER_DELETED;
    }
}
