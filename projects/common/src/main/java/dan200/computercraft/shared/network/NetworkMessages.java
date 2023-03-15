// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

/**
 * Registry for all packets provided by CC: Tweaked.
 *
 * @see PlatformHelper The platform helper is used to send packets.
 */
public final class NetworkMessages {
    private NetworkMessages() {
    }

    public interface PacketRegistry {
        <T extends NetworkMessage<ClientNetworkContext>> void registerClientbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder);

        <T extends NetworkMessage<ServerNetworkContext>> void registerServerbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder);
    }

    public static void register(PacketRegistry registry) {
        // Server messages
        registry.registerServerbound(0, ComputerActionServerMessage.class, ComputerActionServerMessage::new);
        registry.registerServerbound(1, QueueEventServerMessage.class, QueueEventServerMessage::new);
        registry.registerServerbound(2, KeyEventServerMessage.class, KeyEventServerMessage::new);
        registry.registerServerbound(3, MouseEventServerMessage.class, MouseEventServerMessage::new);
        registry.registerServerbound(4, UploadFileMessage.class, UploadFileMessage::new);

        // Client messages
        registry.registerClientbound(10, ChatTableClientMessage.class, ChatTableClientMessage::new);
        registry.registerClientbound(11, PocketComputerDataMessage.class, PocketComputerDataMessage::new);
        registry.registerClientbound(12, PocketComputerDeletedClientMessage.class, PocketComputerDeletedClientMessage::new);
        registry.registerClientbound(13, ComputerTerminalClientMessage.class, ComputerTerminalClientMessage::new);
        registry.registerClientbound(14, PlayRecordClientMessage.class, PlayRecordClientMessage::new);
        registry.registerClientbound(15, MonitorClientMessage.class, MonitorClientMessage::new);
        registry.registerClientbound(16, SpeakerAudioClientMessage.class, SpeakerAudioClientMessage::new);
        registry.registerClientbound(17, SpeakerMoveClientMessage.class, SpeakerMoveClientMessage::new);
        registry.registerClientbound(18, SpeakerPlayClientMessage.class, SpeakerPlayClientMessage::new);
        registry.registerClientbound(19, SpeakerStopClientMessage.class, SpeakerStopClientMessage::new);
        registry.registerClientbound(20, UploadResultMessage.class, UploadResultMessage::new);
        registry.registerClientbound(21, UpgradesLoadedMessage.class, UpgradesLoadedMessage::new);
    }
}
