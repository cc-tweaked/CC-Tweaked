// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@link MessageType} implementation for Forge.
 * <p>
 * This wraps {@link NetworkMessage}s into a {@link CustomPacketPayload}, allowing us to easily use Minecraft's existing
 * custom packets.
 *
 * @param id     The id of this message.
 * @param reader Read this message from a network buffer.
 * @param <T>    The type of our {@link NetworkMessage}.
 */
public record ForgeMessageType<T extends NetworkMessage<?>>(
    ResourceLocation id, FriendlyByteBuf.Reader<Payload<T>> reader
) implements MessageType<T> {
    public static <T extends NetworkMessage<?>> ForgeMessageType<T> cast(MessageType<T> type) {
        return (ForgeMessageType<T>) type;
    }

    public static CustomPacketPayload createPayload(NetworkMessage<?> message) {
        return new Payload<>(message);
    }

    public record Payload<T extends NetworkMessage<?>>(T payload) implements CustomPacketPayload {
        @Override
        public void write(FriendlyByteBuf buf) {
            payload().write(buf);
        }

        @Override
        public ResourceLocation id() {
            return payload().type().id();
        }
    }
}
