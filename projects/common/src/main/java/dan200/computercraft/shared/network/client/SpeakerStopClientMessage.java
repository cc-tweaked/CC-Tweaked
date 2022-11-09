/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * Stops a sound on the client
 * <p>
 * Called when a speaker is broken.
 *
 * @see SpeakerBlockEntity
 */
public class SpeakerStopClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;

    public SpeakerStopClientMessage(UUID source) {
        this.source = source;
    }

    public SpeakerStopClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(source);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerStop(source);
    }
}
