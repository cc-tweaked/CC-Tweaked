/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.UUID;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerAudioClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;
    private final SpeakerPosition.Message pos;
    private final @Nullable ByteBuffer content;
    private final float volume;

    public SpeakerAudioClientMessage(UUID source, SpeakerPosition pos, float volume, ByteBuffer content) {
        this.source = source;
        this.pos = pos.asMessage();
        this.content = content;
        this.volume = volume;
    }

    public SpeakerAudioClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read(buf);
        volume = buf.readFloat();

        // TODO: Remove this, so we no longer need a getter for ClientNetworkContext. However, doing so without
        //  leaking or redundantly copying the buffer is hard.
        ClientNetworkContext.get().handleSpeakerAudioPush(source, buf);
        content = null;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(source);
        pos.write(buf);
        buf.writeFloat(volume);
        buf.writeBytes(assertNonNull(content).duplicate());
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerAudio(source, pos, volume);
    }
}
