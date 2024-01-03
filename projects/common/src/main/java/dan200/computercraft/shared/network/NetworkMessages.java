// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import dan200.computercraft.shared.platform.PlatformHelper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * List of all {@link MessageType}s provided by CC: Tweaked.
 *
 * @see PlatformHelper The platform helper is used to send packets.
 */
public final class NetworkMessages {
    private static final IntSet seenIds = new IntOpenHashSet();
    private static final Set<String> seenChannel = new HashSet<>();
    private static final List<MessageType<? extends NetworkMessage<ServerNetworkContext>>> serverMessages = new ArrayList<>();
    private static final List<MessageType<? extends NetworkMessage<ClientNetworkContext>>> clientMessages = new ArrayList<>();

    public static final MessageType<ComputerActionServerMessage> COMPUTER_ACTION = registerServerbound(0, "computer_action", ComputerActionServerMessage.class, ComputerActionServerMessage::new);
    public static final MessageType<QueueEventServerMessage> QUEUE_EVENT = registerServerbound(1, "queue_event", QueueEventServerMessage.class, QueueEventServerMessage::new);
    public static final MessageType<KeyEventServerMessage> KEY_EVENT = registerServerbound(2, "key_event", KeyEventServerMessage.class, KeyEventServerMessage::new);
    public static final MessageType<MouseEventServerMessage> MOUSE_EVENT = registerServerbound(3, "mouse_event", MouseEventServerMessage.class, MouseEventServerMessage::new);
    public static final MessageType<UploadFileMessage> UPLOAD_FILE = registerServerbound(4, "upload_file", UploadFileMessage.class, UploadFileMessage::new);

    public static final MessageType<ChatTableClientMessage> CHAT_TABLE = registerClientbound(10, "chat_table", ChatTableClientMessage.class, ChatTableClientMessage::new);
    public static final MessageType<PocketComputerDataMessage> POCKET_COMPUTER_DATA = registerClientbound(11, "pocket_computer_data", PocketComputerDataMessage.class, PocketComputerDataMessage::new);
    public static final MessageType<PocketComputerDeletedClientMessage> POCKET_COMPUTER_DELETED = registerClientbound(12, "pocket_computer_deleted", PocketComputerDeletedClientMessage.class, PocketComputerDeletedClientMessage::new);
    public static final MessageType<ComputerTerminalClientMessage> COMPUTER_TERMINAL = registerClientbound(13, "computer_terminal", ComputerTerminalClientMessage.class, ComputerTerminalClientMessage::new);
    public static final MessageType<PlayRecordClientMessage> PLAY_RECORD = registerClientbound(14, "play_record", PlayRecordClientMessage.class, PlayRecordClientMessage::new);
    public static final MessageType<MonitorClientMessage> MONITOR_CLIENT = registerClientbound(15, "monitor_client", MonitorClientMessage.class, MonitorClientMessage::new);
    public static final MessageType<SpeakerAudioClientMessage> SPEAKER_AUDIO = registerClientbound(16, "speaker_audio", SpeakerAudioClientMessage.class, SpeakerAudioClientMessage::new);
    public static final MessageType<SpeakerMoveClientMessage> SPEAKER_MOVE = registerClientbound(17, "speaker_move", SpeakerMoveClientMessage.class, SpeakerMoveClientMessage::new);
    public static final MessageType<SpeakerPlayClientMessage> SPEAKER_PLAY = registerClientbound(18, "speaker_play", SpeakerPlayClientMessage.class, SpeakerPlayClientMessage::new);
    public static final MessageType<SpeakerStopClientMessage> SPEAKER_STOP = registerClientbound(19, "speaker_stop", SpeakerStopClientMessage.class, SpeakerStopClientMessage::new);
    public static final MessageType<UploadResultMessage> UPLOAD_RESULT = registerClientbound(20, "upload_result", UploadResultMessage.class, UploadResultMessage::new);
    public static final MessageType<UpgradesLoadedMessage> UPGRADES_LOADED = registerClientbound(21, "upgrades_loaded", UpgradesLoadedMessage.class, UpgradesLoadedMessage::new);

    private NetworkMessages() {
    }

    private static <C, T extends NetworkMessage<C>> MessageType<T> register(
        List<MessageType<? extends NetworkMessage<C>>> messages,
        int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader
    ) {
        if (!seenIds.add(id)) throw new IllegalArgumentException("Duplicate id " + id);
        if (!seenChannel.add(channel)) throw new IllegalArgumentException("Duplicate channel " + channel);
        var type = PlatformHelper.get().createMessageType(id, new ResourceLocation(ComputerCraftAPI.MOD_ID, channel), klass, reader);
        messages.add(type);
        return type;
    }

    private static <T extends NetworkMessage<ServerNetworkContext>> MessageType<T> registerServerbound(int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
        return register(serverMessages, id, channel, klass, reader);
    }

    private static <T extends NetworkMessage<ClientNetworkContext>> MessageType<T> registerClientbound(int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
        return register(clientMessages, id, channel, klass, reader);
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
