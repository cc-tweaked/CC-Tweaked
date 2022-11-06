/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UploadResultMessage implements NetworkMessage<ClientNetworkContext> {
    private final int containerId;
    private final UploadResult result;
    private final Component errorMessage;

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

    public UploadResultMessage(@Nonnull FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        result = buf.readEnum(UploadResult.class);
        errorMessage = result == UploadResult.ERROR ? buf.readComponent() : null;
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeEnum(result);
        if (result == UploadResult.ERROR) buf.writeComponent(errorMessage);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleUploadResult(containerId, result, errorMessage);
    }
}
