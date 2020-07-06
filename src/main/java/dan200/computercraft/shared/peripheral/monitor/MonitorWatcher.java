/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.MonitorClientMessage;
import dan200.computercraft.shared.network.client.TerminalState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class MonitorWatcher
{
    private static final Queue<TileMonitor> watching = new ArrayDeque<>();

    private MonitorWatcher()
    {
    }

    static void enqueue( TileMonitor monitor )
    {
        if( monitor.enqueued ) return;

        monitor.enqueued = true;
        monitor.cached = null;
        watching.add( monitor );
    }

    @SubscribeEvent
    public static void onWatch( ChunkWatchEvent.Watch event )
    {
        ChunkPos chunkPos = event.getPos();
        Chunk chunk = (Chunk) event.getWorld().getChunk( chunkPos.x, chunkPos.z, ChunkStatus.FULL, false );
        if( chunk == null ) return;

        for( TileEntity te : chunk.getTileEntityMap().values() )
        {
            // Find all origin monitors who are not already on the queue.
            if( !(te instanceof TileMonitor) ) continue;

            TileMonitor monitor = (TileMonitor) te;
            ServerMonitor serverMonitor = getMonitor( monitor );
            if( serverMonitor == null || monitor.enqueued ) continue;

            // We use the cached terminal state if available - this is guaranteed to
            TerminalState state = monitor.cached;
            if( state == null ) state = monitor.cached = serverMonitor.write();
            NetworkHandler.sendToPlayer( event.getPlayer(), new MonitorClientMessage( monitor.getPos(), state ) );
        }
    }

    @SubscribeEvent
    public static void onTick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.END ) return;

        long limit = ComputerCraft.monitorBandwidth;
        boolean obeyLimit = limit > 0;

        TileMonitor tile;
        while( (!obeyLimit || limit > 0) && (tile = watching.poll()) != null )
        {
            tile.enqueued = false;
            ServerMonitor monitor = getMonitor( tile );
            if( monitor == null ) continue;

            BlockPos pos = tile.getPos();
            World world = tile.getWorld();
            if( !(world instanceof ServerWorld) ) continue;

            Chunk chunk = world.getChunkAt( pos );
            if( !((ServerWorld) world).getChunkProvider().chunkManager.getTrackingPlayers( chunk.getPos(), false ).findAny().isPresent() )
            {
                continue;
            }

            TerminalState state = tile.cached = monitor.write();
            NetworkHandler.sendToAllTracking( new MonitorClientMessage( pos, state ), chunk );

            limit -= state.size();
        }
    }

    private static ServerMonitor getMonitor( TileMonitor monitor )
    {
        return !monitor.isRemoved() && monitor.getXIndex() == 0 && monitor.getYIndex() == 0 ? monitor.getCachedServerMonitor() : null;
    }
}
