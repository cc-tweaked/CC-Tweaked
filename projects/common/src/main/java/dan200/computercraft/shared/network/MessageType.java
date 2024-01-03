// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network;

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
}
