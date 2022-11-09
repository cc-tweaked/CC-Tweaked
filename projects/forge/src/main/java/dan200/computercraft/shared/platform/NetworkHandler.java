/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.network.server.ServerNetworkContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Function;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public final class NetworkHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);

    private static final SimpleChannel network;

    static {
        var version = ComputerCraftAPI.getInstalledVersion();
        network = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(ComputerCraftAPI.MOD_ID, "network"))
            .networkProtocolVersion(() -> version)
            .clientAcceptedVersions(version::equals).serverAcceptedVersions(version::equals)
            .simpleChannel();
    }

    private NetworkHandler() {
    }

    public static void setup() {
        IntSet usedIds = new IntOpenHashSet();
        NetworkMessages.register(new NetworkMessages.PacketRegistry() {
            @Override
            public <T extends NetworkMessage<ClientNetworkContext>> void registerClientbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                if (!usedIds.add(id)) throw new IllegalArgumentException("Already have a packet with id " + id);
                registerMainThread(id, NetworkDirection.PLAY_TO_CLIENT, type, decoder, x -> ClientNetworkContext.get());
            }

            @Override
            public <T extends NetworkMessage<ServerNetworkContext>> void registerServerbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
                if (!usedIds.add(id)) throw new IllegalArgumentException("Already have a packet with id " + id);
                registerMainThread(id, NetworkDirection.PLAY_TO_SERVER, type, decoder, c -> () -> assertNonNull(c.getSender()));
            }
        });
    }

    static void sendToPlayer(NetworkMessage<ClientNetworkContext> packet, ServerPlayer player) {
        network.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    static void sendToPlayers(NetworkMessage<ClientNetworkContext> packet, Collection<ServerPlayer> players) {
        if (players.isEmpty()) return;

        var vanillaPacket = network.toVanillaPacket(packet, NetworkDirection.PLAY_TO_CLIENT);
        for (var player : players) player.connection.send(vanillaPacket);
    }

    static void sendToAllPlayers(NetworkMessage<ClientNetworkContext> packet) {
        network.send(PacketDistributor.ALL.noArg(), packet);
    }

    static void sendToAllAround(NetworkMessage<ClientNetworkContext> packet, Level world, Vec3 pos, double range) {
        var target = new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, range, world.dimension());
        network.send(PacketDistributor.NEAR.with(() -> target), packet);
    }

    static void sendToAllTracking(NetworkMessage<ClientNetworkContext> packet, LevelChunk chunk) {
        network.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
    }

    public static void sendToServer(NetworkMessage<ServerNetworkContext> packet) {
        network.sendToServer(packet);
    }

    /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param <T>       The type of the packet to send.
     * @param <H>       The context this packet is evaluated under.
     * @param type      The class of the type of packet to send.
     * @param id        The identifier for this packet type.
     * @param direction A network direction which will be asserted before any processing of this message occurs
     * @param decoder   The factory for this type of packet.
     * @param handler   Gets or constructs the handler for this packet.
     */
    static <H, T extends NetworkMessage<H>> void registerMainThread(
        int id, NetworkDirection direction, Class<T> type, Function<FriendlyByteBuf, T> decoder,
        Function<NetworkEvent.Context, H> handler
    ) {
        network.messageBuilder(type, id, direction)
            .encoder(NetworkMessage::toBytes)
            .decoder(decoder)
            .consumerMainThread((packet, contextSup) -> {
                try {
                    packet.handle(handler.apply(contextSup.get()));
                } catch (RuntimeException | Error e) {
                    LOG.error("Failed handling packet", e);
                    throw e;
                }
            })
            .add();
    }
}
