// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;


public class PocketComputerDeletedClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final int instanceId;

    public PocketComputerDeletedClientMessage(int instanceId) {
        this.instanceId = instanceId;
    }

    public PocketComputerDeletedClientMessage(FriendlyByteBuf buffer) {
        instanceId = buffer.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(instanceId);
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
