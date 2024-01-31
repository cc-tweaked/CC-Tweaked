// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * List of all {@link MessageType}s provided by CC: Tweaked.
 *
 * @see PlatformHelper The platform helper is used to send packets.
 */
public final class NetworkMessages {
    private static final Set<String> seenChannel = new HashSet<>();
    private static final List<MessageType<? extends NetworkMessage<ServerNetworkContext>>> serverMessages = new ArrayList<>();
    private static final List<MessageType<? extends NetworkMessage<ClientNetworkContext>>> clientMessages = new ArrayList<>();

    public static final MessageType<ComputerActionServerMessage> COMPUTER_ACTION = registerServerbound("computer_action", ComputerActionServerMessage::new);
    public static final MessageType<QueueEventServerMessage> QUEUE_EVENT = registerServerbound("queue_event", QueueEventServerMessage::new);
    public static final MessageType<KeyEventServerMessage> KEY_EVENT = registerServerbound("key_event", KeyEventServerMessage::new);
    public static final MessageType<MouseEventServerMessage> MOUSE_EVENT = registerServerbound("mouse_event", MouseEventServerMessage::new);
    public static final MessageType<UploadFileMessage> UPLOAD_FILE = registerServerbound("upload_file", UploadFileMessage::new);

    public static final MessageType<ChatTableClientMessage> CHAT_TABLE = registerClientbound("chat_table", ChatTableClientMessage::new);
    public static final MessageType<PocketComputerDataMessage> POCKET_COMPUTER_DATA = registerClientbound("pocket_computer_data", PocketComputerDataMessage::new);
    public static final MessageType<PocketComputerDeletedClientMessage> POCKET_COMPUTER_DELETED = registerClientbound("pocket_computer_deleted", PocketComputerDeletedClientMessage::new);
    public static final MessageType<ComputerTerminalClientMessage> COMPUTER_TERMINAL = registerClientbound("computer_terminal", ComputerTerminalClientMessage::new);
    public static final MessageType<PlayRecordClientMessage> PLAY_RECORD = registerClientbound("play_record", PlayRecordClientMessage::new);
    public static final MessageType<MonitorClientMessage> MONITOR_CLIENT = registerClientbound("monitor_client", MonitorClientMessage::new);
    public static final MessageType<SpeakerAudioClientMessage> SPEAKER_AUDIO = registerClientbound("speaker_audio", SpeakerAudioClientMessage::new);
    public static final MessageType<SpeakerMoveClientMessage> SPEAKER_MOVE = registerClientbound("speaker_move", SpeakerMoveClientMessage::new);
    public static final MessageType<SpeakerPlayClientMessage> SPEAKER_PLAY = registerClientbound("speaker_play", SpeakerPlayClientMessage::new);
    public static final MessageType<SpeakerStopClientMessage> SPEAKER_STOP = registerClientbound("speaker_stop", SpeakerStopClientMessage::new);
    public static final MessageType<UploadResultMessage> UPLOAD_RESULT = registerClientbound("upload_result", UploadResultMessage::new);
    public static final MessageType<UpgradesLoadedMessage> UPGRADES_LOADED = registerClientbound("upgrades_loaded", UpgradesLoadedMessage::new);

    private NetworkMessages() {
    }

    private static <C, T extends NetworkMessage<C>> MessageType<T> register(
        List<MessageType<? extends NetworkMessage<C>>> messages,
        String channel, FriendlyByteBuf.Reader<T> reader
    ) {
        if (!seenChannel.add(channel)) throw new IllegalArgumentException("Duplicate channel " + channel);
        var type = PlatformHelper.get().createMessageType(new ResourceLocation(ComputerCraftAPI.MOD_ID, channel), reader);
        messages.add(type);
        return type;
    }

    private static <T extends NetworkMessage<ServerNetworkContext>> MessageType<T> registerServerbound(String id, FriendlyByteBuf.Reader<T> reader) {
        return register(serverMessages, id, reader);
    }

    private static <T extends NetworkMessage<ClientNetworkContext>> MessageType<T> registerClientbound(String id, FriendlyByteBuf.Reader<T> reader) {
        return register(clientMessages, id, reader);
    }

    /**
     * Get all serverbound message types.
     *
     * @return An unmodifiable sequence of all serverbound message types.
     */
    public static Collection<MessageType<? extends NetworkMessage<ServerNetworkContext>>> getServerbound() {
        return Collections.unmodifiableCollection(serverMessages);
    }

    /**
     * Get all clientbound message types.
     *
     * @return An unmodifiable sequence of all clientbound message types.
     */
    public static Collection<MessageType<? extends NetworkMessage<ClientNetworkContext>>> getClientbound() {
        return Collections.unmodifiableCollection(clientMessages);
    }
}
