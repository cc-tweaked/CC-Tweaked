/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.collect.MapMaker;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * We use this when modems and other peripherals change a block in a different thread.
 */
public final class TickScheduler
{
    private static final Set<BlockEntity> toTick = Collections.newSetFromMap( new MapMaker().weakKeys()
        .makeMap() );

    private TickScheduler()
    {
    }

    public static void schedule( TileGeneric tile )
    {
        Level world = tile.getLevel();
        if( world != null && !world.isClientSide )
        {
            toTick.add( tile );
        }
    }

    public static void tick()
    {
        Iterator<BlockEntity> iterator = toTick.iterator();
        while( iterator.hasNext() )
        {
            BlockEntity tile = iterator.next();
            iterator.remove();

            Level world = tile.getLevel();
            BlockPos pos = tile.getBlockPos();

            if( world != null && pos != null && world.hasChunkAt( pos ) && world.getBlockEntity( pos ) == tile )
            {
                world.getBlockTicks()
                    .scheduleTick( pos,
                        tile.getBlockState()
                            .getBlock(),
                        0 );
            }
        }
    }
}
