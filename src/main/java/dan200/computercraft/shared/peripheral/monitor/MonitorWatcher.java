/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.client.MonitorClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Queue;

public final class MonitorWatcher {
    private static final Queue<TileMonitor> watching = new ArrayDeque<>();

    private MonitorWatcher() {
    }

    static void enqueue(TileMonitor monitor) {
        if (monitor.enqueued) return;

        monitor.enqueued = true;
        monitor.cached = null;
        watching.add(monitor);
    }

    public static void onWatch(LevelChunk chunk, ServerPlayer player) {
        // Find all origin monitors who are not already on the queue and send the
        // monitor data to the player.
        for (var te : chunk.getBlockEntities().values()) {
            if (!(te instanceof TileMonitor monitor)) continue;

            var serverMonitor = getMonitor(monitor);
            if (serverMonitor == null || monitor.enqueued) continue;

            var state = getState(monitor, serverMonitor);
            PlatformHelper.get().sendToPlayer(new MonitorClientMessage(monitor.getBlockPos(), state), player);
        }
    }

    public static void onTick() {
        // Find all enqueued monitors and send their contents to all nearby players.

        var limit = ComputerCraft.monitorBandwidth;
        var obeyLimit = limit > 0;

        TileMonitor tile;
        while ((!obeyLimit || limit > 0) && (tile = watching.poll()) != null) {
            tile.enqueued = false;
            var monitor = getMonitor(tile);
            if (monitor == null) continue;

            var pos = tile.getBlockPos();
            var world = tile.getLevel();
            if (!(world instanceof ServerLevel)) continue;

            var chunk = world.getChunkAt(pos);
            if (((ServerLevel) world).getChunkSource().chunkMap.getPlayers(chunk.getPos(), false).isEmpty()) {
                continue;
            }

            var state = getState(tile, monitor);
            PlatformHelper.get().sendToAllTracking(new MonitorClientMessage(pos, state), chunk);

            limit -= state.size();
        }
    }

    private static ServerMonitor getMonitor(TileMonitor monitor) {
        return !monitor.isRemoved() && monitor.getXIndex() == 0 && monitor.getYIndex() == 0 ? monitor.getCachedServerMonitor() : null;
    }

    private static TerminalState getState(TileMonitor tile, ServerMonitor monitor) {
        var state = tile.cached;
        if (state == null) state = tile.cached = new TerminalState(monitor.getTerminal());
        return state;
    }
}
