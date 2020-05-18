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
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

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
        Chunk chunk = event.getChunkInstance();
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
            WorldServer serverWorld = world instanceof WorldServer ? (WorldServer) world : DimensionManager.getWorld( world.provider.getDimension() );
            PlayerChunkMapEntry entry = serverWorld.getPlayerChunkMap().getEntry( pos.getX() >> 4, pos.getZ() >> 4 );
            if( entry == null || entry.getWatchingPlayers().isEmpty() ) continue;

            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint( world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0 );
            TerminalState state = tile.cached = monitor.write();
            NetworkHandler.sendToAllTracking( new MonitorClientMessage( pos, state ), point );

            limit -= state.size();
        }
    }

    private static ServerMonitor getMonitor( TileMonitor monitor )
    {
        return !monitor.isInvalid() && monitor.getXIndex() == 0 && monitor.getYIndex() == 0 ? monitor.getCachedServerMonitor() : null;
    }
}
