// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Methods for sending network messages from the server to clients.
 */
public final class ServerNetworking {
    private ServerNetworking() {
    }

    /**
     * Send a message to a specific player.
     *
     * @param message The message to send.
     * @param player  The player to send it to.
     */
    public static void sendToPlayer(NetworkMessage<ClientNetworkContext> message, ServerPlayer player) {
        player.connection.send(PlatformHelper.get().createPacket(message));
    }

    /**
     * Send a message to a set of players.
     *
     * @param message The message to send.
     * @param players The players to send it to.
     */
    public static void sendToPlayers(NetworkMessage<ClientNetworkContext> message, Collection<ServerPlayer> players) {
        if (players.isEmpty()) return;
        var packet = PlatformHelper.get().createPacket(message);
        for (var player : players) player.connection.send(packet);
    }

    /**
     * Send a message to all players.
     *
     * @param message The message to send.
     * @param server  The current server.
     */
    public static void sendToAllPlayers(NetworkMessage<ClientNetworkContext> message, MinecraftServer server) {
        server.getPlayerList().broadcastAll(PlatformHelper.get().createPacket(message));
    }

    /**
     * Send a message to all players around a point.
     *
     * @param message  The message to send.
     * @param level    The level the point is in.
     * @param pos      The centre position.
     * @param distance The distance to the centre players must be within.
     */
    public static void sendToAllAround(NetworkMessage<ClientNetworkContext> message, ServerLevel level, Vec3 pos, float distance) {
        level.getServer().getPlayerList().broadcast(null, pos.x, pos.y, pos.z, distance, level.dimension(), PlatformHelper.get().createPacket(message));
    }

    /**
     * Send a message to all players tracking a chunk.
     *
     * @param message The message to send.
     * @param chunk   The chunk players must be tracking.
     */
    public static void sendToAllTracking(NetworkMessage<ClientNetworkContext> message, LevelChunk chunk) {
        var packet = PlatformHelper.get().createPacket(message);
        for (var player : ((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false)) {
            player.connection.send(packet);
        }
    }
}
