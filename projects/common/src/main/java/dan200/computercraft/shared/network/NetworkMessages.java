// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * List of all {@link CustomPacketPayload.Type}s provided by CC: Tweaked.
 *
 * @see PlatformHelper The platform helper is used to send packets.
 */
public final class NetworkMessages {
    private static final Set<String> seenChannel = new HashSet<>();
    private static final List<CustomPacketPayload.TypeAndCodec<RegistryFriendlyByteBuf, ? extends NetworkMessage<ServerNetworkContext>>> serverMessages = new ArrayList<>();
    private static final List<CustomPacketPayload.TypeAndCodec<RegistryFriendlyByteBuf, ? extends NetworkMessage<ClientNetworkContext>>> clientMessages = new ArrayList<>();

    public static final CustomPacketPayload.Type<ComputerActionServerMessage> COMPUTER_ACTION = registerServerbound("computer_action", ComputerActionServerMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<QueueEventServerMessage> QUEUE_EVENT = register(serverMessages, "queue_event", QueueEventServerMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<KeyEventServerMessage> KEY_EVENT = registerServerbound("key_event", KeyEventServerMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<MouseEventServerMessage> MOUSE_EVENT = registerServerbound("mouse_event", MouseEventServerMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<UploadFileMessage> UPLOAD_FILE = register(serverMessages, "upload_file", UploadFileMessage.STREAM_CODEC);

    public static final CustomPacketPayload.Type<ChatTableClientMessage> CHAT_TABLE = registerClientbound("chat_table", ChatTableClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<PocketComputerDataMessage> POCKET_COMPUTER_DATA = registerClientbound("pocket_computer_data", PocketComputerDataMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<PocketComputerDeletedClientMessage> POCKET_COMPUTER_DELETED = registerClientbound("pocket_computer_deleted", PocketComputerDeletedClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<ComputerTerminalClientMessage> COMPUTER_TERMINAL = registerClientbound("computer_terminal", ComputerTerminalClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<PlayRecordClientMessage> PLAY_RECORD = registerClientbound("play_record", PlayRecordClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<MonitorClientMessage> MONITOR_CLIENT = registerClientbound("monitor_client", MonitorClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<SpeakerAudioClientMessage> SPEAKER_AUDIO = registerClientbound("speaker_audio", SpeakerAudioClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<SpeakerMoveClientMessage> SPEAKER_MOVE = registerClientbound("speaker_move", SpeakerMoveClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<SpeakerPlayClientMessage> SPEAKER_PLAY = registerClientbound("speaker_play", SpeakerPlayClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<SpeakerStopClientMessage> SPEAKER_STOP = registerClientbound("speaker_stop", SpeakerStopClientMessage.STREAM_CODEC);
    public static final CustomPacketPayload.Type<UploadResultMessage> UPLOAD_RESULT = registerClientbound("upload_result", UploadResultMessage.STREAM_CODEC);

    private NetworkMessages() {
    }

    private static <C, T extends NetworkMessage<C>> CustomPacketPayload.Type<T> register(
        List<CustomPacketPayload.TypeAndCodec<RegistryFriendlyByteBuf, ? extends NetworkMessage<C>>> messages,
        String channel, StreamCodec<RegistryFriendlyByteBuf, T> codec
    ) {
        if (!seenChannel.add(channel)) throw new IllegalArgumentException("Duplicate channel " + channel);
        var type = new CustomPacketPayload.Type<T>(new ResourceLocation(ComputerCraftAPI.MOD_ID, channel));
        messages.add(new CustomPacketPayload.TypeAndCodec<>(type, codec));
        return type;
    }

    private static <T extends NetworkMessage<ServerNetworkContext>> CustomPacketPayload.Type<T> registerServerbound(String id, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return register(serverMessages, id, codec);
    }

    private static <T extends NetworkMessage<ClientNetworkContext>> CustomPacketPayload.Type<T> registerClientbound(String id, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return register(clientMessages, id, codec);
    }

    /**
     * Get all serverbound message types.
     *
     * @return An unmodifiable sequence of all serverbound message types.
     */
    public static Collection<CustomPacketPayload.TypeAndCodec<RegistryFriendlyByteBuf, ? extends NetworkMessage<ServerNetworkContext>>> getServerbound() {
        return Collections.unmodifiableCollection(serverMessages);
    }

    /**
     * Get all clientbound message types.
     *
     * @return An unmodifiable sequence of all clientbound message types.
     */
    public static Collection<CustomPacketPayload.TypeAndCodec<RegistryFriendlyByteBuf, ? extends NetworkMessage<ClientNetworkContext>>> getClientbound() {
        return Collections.unmodifiableCollection(clientMessages);
    }
}
