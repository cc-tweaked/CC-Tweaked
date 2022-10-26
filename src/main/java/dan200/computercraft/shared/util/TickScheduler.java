/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A thread-safe version of {@link LevelAccessor#scheduleTick(BlockPos, Block, int)}.
 * <p>
 * We use this when modems and other peripherals change a block in a different thread.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class TickScheduler
{
    private TickScheduler()
    {
    }

    private static final Queue<TileGeneric> toTick = new ConcurrentLinkedDeque<>();

    public static void schedule( TileGeneric tile )
    {
        Level world = tile.getLevel();
        if( world != null && !world.isClientSide && !tile.scheduled.getAndSet( true ) ) toTick.add( tile );
    }

    @SubscribeEvent
    public static void tick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        TileGeneric tile;
        while( (tile = toTick.poll()) != null )
        {
            tile.scheduled.set( false );
            if( tile.isRemoved() ) continue;

            Level world = tile.getLevel();
            BlockPos pos = tile.getBlockPos();

            if( world != null && pos != null && world.isLoaded( pos ) && world.getBlockEntity( pos ) == tile )
            {
                world.scheduleTick( pos, tile.getBlockState().getBlock(), 0 );
            }
        }
    }
}
