// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * An implementation of {@link MessageType} for Fabric.
 * <p>
 * This provides conversions between the {@link FabricPacket}/{@link PacketType} and {@link NetworkMessage}/{@link MessageType}
 * interfaces, allowing us to interop between the two.
 *
 * @param type The underlying {@link PacketType}
 * @param <T>  The type of the message.
 */
public record FabricMessageType<T extends NetworkMessage<?>>(
    PacketType<PacketWrapper<T>> type
) implements MessageType<T> {
    public FabricMessageType(ResourceLocation id, Function<FriendlyByteBuf, T> reader) {
        this(PacketType.create(id, b -> new PacketWrapper<>(reader.apply(b))));
    }

    public static <T extends NetworkMessage<?>> PacketType<PacketWrapper<T>> toFabricType(MessageType<T> type) {
        return ((FabricMessageType<T>) type).type();
    }

    public static FabricPacket toFabricPacket(NetworkMessage<?> message) {
        return new PacketWrapper<>(message);
    }

    public record PacketWrapper<T extends NetworkMessage<?>>(T payload) implements FabricPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            payload().write(buf);
        }

        @Override
        public PacketType<?> getType() {
            return FabricMessageType.toFabricType(payload().type());
        }
    }
}
