/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread-safe version of {@link LevelAccessor#scheduleTick(BlockPos, Block, int)}.
 * <p>
 * We use this when modems and other peripherals change a block in a different thread.
 */
@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID)
public final class TickScheduler {
    private TickScheduler() {
    }

    private static final Queue<Token> toTick = new ConcurrentLinkedDeque<>();

    public static void schedule(Token token) {
        var world = token.owner.getLevel();
        if (world != null && !world.isClientSide && !token.scheduled.getAndSet(true)) toTick.add(token);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Token token;
        while ((token = toTick.poll()) != null) {
            token.scheduled.set(false);
            var blockEntity = token.owner;
            if (blockEntity.isRemoved()) continue;

            var world = blockEntity.getLevel();
            var pos = blockEntity.getBlockPos();

            if (world != null && world.isLoaded(pos) && world.getBlockEntity(pos) == blockEntity) {
                world.scheduleTick(pos, blockEntity.getBlockState().getBlock(), 0);
            }
        }
    }

    /**
     * An item which can be scheduled for future ticking.
     * <p>
     * This tracks whether the {@link BlockEntity} is queued or not, as this is more efficient than maintaining a set.
     * As such, it should be unique per {@link BlockEntity} instance to avoid it being queued multiple times.
     */
    public static class Token {
        final BlockEntity owner;
        final AtomicBoolean scheduled = new AtomicBoolean();

        public Token(BlockEntity owner) {
            this.owner = owner;
        }
    }
}
