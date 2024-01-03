// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public class UploadResultMessage implements NetworkMessage<ClientNetworkContext> {
    private final int containerId;
    private final UploadResult result;
    private final @Nullable Component errorMessage;

    private UploadResultMessage(AbstractContainerMenu container, UploadResult result, @Nullable Component errorMessage) {
        containerId = container.containerId;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static UploadResultMessage queued(AbstractContainerMenu container) {
        return new UploadResultMessage(container, UploadResult.QUEUED, null);
    }

    public static UploadResultMessage consumed(AbstractContainerMenu container) {
        return new UploadResultMessage(container, UploadResult.CONSUMED, null);
    }

    public static UploadResultMessage error(AbstractContainerMenu container, Component errorMessage) {
        return new UploadResultMessage(container, UploadResult.ERROR, errorMessage);
    }

    public UploadResultMessage(FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        result = buf.readEnum(UploadResult.class);
        errorMessage = result == UploadResult.ERROR ? buf.readComponent() : null;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeEnum(result);
        if (result == UploadResult.ERROR) buf.writeComponent(Nullability.assertNonNull(errorMessage));
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleUploadResult(containerId, result, errorMessage);
    }

    @Override
    public MessageType<UploadResultMessage> type() {
        return NetworkMessages.UPLOAD_RESULT;
    }
}
