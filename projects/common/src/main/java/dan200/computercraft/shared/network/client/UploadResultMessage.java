// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Optional;

public final class UploadResultMessage implements NetworkMessage<ClientNetworkContext> {
    public static final StreamCodec<RegistryFriendlyByteBuf, UploadResultMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, x -> x.containerId,
        MoreStreamCodecs.ofEnum(UploadResult.class), x -> x.result,
        ComponentSerialization.OPTIONAL_STREAM_CODEC, x -> x.errorMessage,
        UploadResultMessage::new
    );

    private final int containerId;
    private final UploadResult result;
    private final Optional<Component> errorMessage;

    private UploadResultMessage(int containerId, UploadResult result, Optional<Component> errorMessage) {
        this.containerId = containerId;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static UploadResultMessage queued(AbstractContainerMenu container) {
        return new UploadResultMessage(container.containerId, UploadResult.QUEUED, Optional.empty());
    }

    public static UploadResultMessage consumed(AbstractContainerMenu container) {
        return new UploadResultMessage(container.containerId, UploadResult.CONSUMED, Optional.empty());
    }

    public static UploadResultMessage error(AbstractContainerMenu container, Component errorMessage) {
        return new UploadResultMessage(container.containerId, UploadResult.ERROR, Optional.of(errorMessage));
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleUploadResult(containerId, result, errorMessage.orElse(null));
    }

    @Override
    public CustomPacketPayload.Type<UploadResultMessage> type() {
        return NetworkMessages.UPLOAD_RESULT;
    }
}
