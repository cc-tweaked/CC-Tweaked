// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Starts a sound on the client.
 * <p>
 * Used by speakers to play sounds.
 *
 * @see SpeakerBlockEntity
 */
public class SpeakerPlayClientMessage implements NetworkMessage<ClientNetworkContext> {
    private final UUID source;
    private final SpeakerPosition.Message pos;
    private final ResourceLocation sound;
    private final float volume;
    private final float pitch;

    public SpeakerPlayClientMessage(UUID source, SpeakerPosition pos, ResourceLocation sound, float volume, float pitch) {
        this.source = source;
        this.pos = pos.asMessage();
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SpeakerPlayClientMessage(FriendlyByteBuf buf) {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read(buf);
        sound = buf.readResourceLocation();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(source);
        pos.write(buf);
        buf.writeResourceLocation(sound);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleSpeakerPlay(source, pos, sound, volume, pitch);
    }

    @Override
    public MessageType<SpeakerPlayClientMessage> type() {
        return NetworkMessages.SPEAKER_PLAY;
    }
}
