// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.server.ServerNetworkContext;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class NetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);

    public static final ResourceLocation ID = new ResourceLocation(ComputerCraftAPI.MOD_ID, "main");

    private static final Int2ObjectMap<Function<FriendlyByteBuf, ? extends NetworkMessage<ClientNetworkContext>>> clientPackets = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<Function<FriendlyByteBuf, ? extends NetworkMessage<ServerNetworkContext>>> serverPackets = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<Class<? extends NetworkMessage<?>>> packetIds = new Object2IntOpenHashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            var packet = decodeServer(buf);
            if (packet != null) server.execute(() -> packet.handle(handler::getPlayer));
        });

        NetworkMessages.register(new NetworkMessages.PacketRegistry() {
            @Override
            public <T extends NetworkMessage<ClientNetworkContext>> void registerClientbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                clientPackets.put(id, decoder);
                packetIds.put(type, id);
            }

            @Override
            public <T extends NetworkMessage<ServerNetworkContext>> void registerServerbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                serverPackets.put(id, decoder);
                packetIds.put(type, id);
            }
        });
    }

    private static FriendlyByteBuf encode(NetworkMessage<?> message) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(packetIds.getInt(message.getClass()));
        message.toBytes(buf);
        return buf;
    }

    public static ClientboundCustomPayloadPacket encodeClient(NetworkMessage<ClientNetworkContext> message) {
        return new ClientboundCustomPayloadPacket(ID, encode(message));
    }

    public static ServerboundCustomPayloadPacket encodeServer(NetworkMessage<ServerNetworkContext> message) {
        return new ServerboundCustomPayloadPacket(ID, encode(message));
    }

    @Nullable
    private static <T> NetworkMessage<T> decode(Int2ObjectMap<Function<FriendlyByteBuf, ? extends NetworkMessage<T>>> packets, FriendlyByteBuf buffer) {
        int type = buffer.readByte();
        var reader = packets.get(type);
        if (reader == null) {
            LOGGER.debug("Unknown packet {}", type);
            return null;
        }

        return reader.apply(buffer);
    }

    @Nullable
    public static NetworkMessage<ServerNetworkContext> decodeServer(FriendlyByteBuf buffer) {
        return decode(serverPackets, buffer);
    }

    @Nullable
    public static NetworkMessage<ClientNetworkContext> decodeClient(FriendlyByteBuf buffer) {
        return decode(clientPackets, buffer);
    }
}
