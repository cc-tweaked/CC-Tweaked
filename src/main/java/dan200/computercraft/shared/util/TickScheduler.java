/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread-safe version of {@link ITickList#scheduleTick(BlockPos, Object, int)}.
 * <p>
 * We use this when modems and other peripherals change a block in a different thread.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class TickScheduler
{
    private TickScheduler()
    {
    }

    private static final Queue<Token> toTick = new ConcurrentLinkedDeque<>();

    public static void schedule( Token token )
    {
        World world = token.owner.getLevel();
        if( world != null && !world.isClientSide && !token.scheduled.getAndSet( true ) ) toTick.add( token );
    }

    @SubscribeEvent
    public static void tick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        Token token;
        while( (token = toTick.poll()) != null )
        {
            token.scheduled.set( false );
            TileEntity blockEntity = token.owner;
            if( blockEntity.isRemoved() ) continue;

            World world = blockEntity.getLevel();
            BlockPos pos = blockEntity.getBlockPos();

            if( world != null && world.isAreaLoaded( pos, 0 ) && world.getBlockEntity( pos ) == blockEntity )
            {
                world.getBlockTicks().scheduleTick( pos, blockEntity.getBlockState().getBlock(), 0 );
            }
        }
    }

    /**
     * An item which can be scheduled for future ticking.
     * <p>
     * This tracks whether the {@link TileEntity} is queued or not, as this is more efficient than maintaining a set.
     * As such, it should be unique per {@link TileEntity} instance to avoid it being queued multiple times.
     */
    public static class Token
    {
        final TileEntity owner;
        final AtomicBoolean scheduled = new AtomicBoolean();

        public Token( TileEntity owner )
        {
            this.owner = owner;
        }
    }
}
