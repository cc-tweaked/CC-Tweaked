// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Delete any client-side pocket computer state.
 *
 * @param instanceId The pocket computer's instance id.
 */
public record PocketComputerDeletedClientMessage(UUID instanceId) implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PocketComputerDeletedClientMessage> STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .map(PocketComputerDeletedClientMessage::new, PocketComputerDeletedClientMessage::instanceId)
        .cast();

    @Override
    public void handle(ClientNetworkContext context) {
        context.handlePocketComputerDeleted(instanceId);
    }

    @Override
    public CustomPacketPayload.Type<PocketComputerDeletedClientMessage> type() {
        return NetworkMessages.POCKET_COMPUTER_DELETED;
    }
}
