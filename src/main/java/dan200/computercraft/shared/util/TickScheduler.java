/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.collect.MapMaker;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * A thread-safe version of {@link ITickList#scheduleTick(BlockPos, Object, int)}.
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
        World world = tile.getLevel();
        if( world != null && !world.isClientSide ) toTick.add( tile );
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

            World world = tile.getLevel();
            BlockPos pos = tile.getBlockPos();

            if( world != null && pos != null && world.isAreaLoaded( pos, 0 ) && world.getBlockEntity( pos ) == tile )
            {
                world.getBlockTicks().scheduleTick( pos, tile.getBlockState().getBlock(), 0 );
            }
        }
    }
}
