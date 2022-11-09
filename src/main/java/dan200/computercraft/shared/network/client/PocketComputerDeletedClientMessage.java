/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
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
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(instanceId);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePocketComputerDeleted(instanceId);
    }
}
