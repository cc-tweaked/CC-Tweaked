/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.collect.MapMaker;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * A thread-safe version of {@link World#scheduleUpdate(BlockPos, Block, int)}.
 *
 * We use this when modems and other peripherals change a block in a different thread.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class TickScheduler
{
    private TickScheduler()
    {
    }

    private static final Set<TileEntity> toTick = Collections.newSetFromMap(
        new MapMaker()
            .weakKeys()
            .makeMap()
    );

    public static void schedule( TileGeneric tile )
    {
        World world = tile.getWorld();
        if( world != null && !world.isRemote ) toTick.add( tile );
    }

    @SubscribeEvent
    public static void tick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        Iterator<TileEntity> iterator = toTick.iterator();
        while( iterator.hasNext() )
        {
            TileEntity tile = iterator.next();
            iterator.remove();

            World world = tile.getWorld();
            BlockPos pos = tile.getPos();

            if( world != null && pos != null && world.isBlockLoaded( pos ) && world.getTileEntity( pos ) == tile )
            {
                world.scheduleUpdate( pos, tile.getBlockType(), 0 );
            }
        }
    }
}
