// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.server.ServerNetworkContext;
import net.minecraft.network.FriendlyByteBuf;


/**
 * The base interface for any message which will be sent to the client or server.
 *
 * @param <T> The context under which packets are evaluated.
 * @see ClientNetworkContext
 * @see ServerNetworkContext
 */
public interface NetworkMessage<T> {
    /**
     * Get the type of this message.
     *
     * @return The type of this message.
     */
    MessageType<?> type();

    /**
     * Write this packet to a buffer.
     * <p>
     * This may be called on any thread, so this should be a pure operation.
     *
     * @param buf The buffer to write data to.
     */
    void write(FriendlyByteBuf buf);

    /**
     * Handle this {@link NetworkMessage}.
     *
     * @param context The context with which to handle this message
     */
    void handle(T context);
}
