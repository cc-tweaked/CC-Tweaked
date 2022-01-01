/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.collect.MapMaker;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * A thread-safe version of {@link LevelAccessor#scheduleTick(BlockPos, Block, int)}.
 *
 * We use this when modems and other peripherals change a block in a different thread.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class TickScheduler
{
    private TickScheduler()
    {
    }

    private static final Set<BlockEntity> toTick = Collections.newSetFromMap(
        new MapMaker()
            .weakKeys()
            .makeMap()
    );

    public static void schedule( TileGeneric tile )
    {
        Level world = tile.getLevel();
        if( world != null && !world.isClientSide ) toTick.add( tile );
    }

    @SubscribeEvent
    public static void tick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        Iterator<BlockEntity> iterator = toTick.iterator();
        while( iterator.hasNext() )
        {
            BlockEntity tile = iterator.next();
            iterator.remove();

            Level world = tile.getLevel();
            BlockPos pos = tile.getBlockPos();

            if( world != null && pos != null && world.isLoaded( pos ) && world.getBlockEntity( pos ) == tile )
            {
                world.scheduleTick( pos, tile.getBlockState().getBlock(), 0 );
            }
        }
    }
}
