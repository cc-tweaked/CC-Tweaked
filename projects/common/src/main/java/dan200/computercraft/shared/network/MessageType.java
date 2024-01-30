// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A type of message to send over the network.
 * <p>
 * Much like recipe or argument serialisers, each type of {@link NetworkMessage} should have a unique type associated
 * with it. This holds platform-specific information about how the packet should be sent over the network.
 *
 * @param <T> The type of message to send
 * @see NetworkMessages
 * @see NetworkMessage#type()
 */
public interface MessageType<T extends NetworkMessage<?>> {
    /**
     * Get the id of this message type. This will be used as the custom packet channel name.
     *
     * @return The id of this message type.
     * @see CustomPacketPayload#id()
     */
    ResourceLocation id();
}
