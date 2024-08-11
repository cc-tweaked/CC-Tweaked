// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.google.errorprone.annotations.Keep;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A thread-safe version of {@link LevelAccessor#scheduleTick(BlockPos, Block, int)}.
 * <p>
 * We use this when modems and other peripherals change a block in a different thread.
 */
public final class TickScheduler {
    private TickScheduler() {
    }

    /**
     * The list of block entities to tick.
     */
    private static final Queue<Token> toTick = new ConcurrentLinkedDeque<>();

    /**
     * Block entities which we want to tick, but whose chunks not currently loaded.
     * <p>
     * Minecraft sometimes keeps chunks in-memory, but not actively loaded. If such a block entity is in the
     * {@link #toTick} queue, we'll see that it's not loaded and so have to skip scheduling a tick.
     * <p>
     * However, if the block entity is ever loaded again, we need to tick it. Unfortunately, block entities in this
     * state are not notified in any way (for instance, {@link BlockEntity#setRemoved()} or
     * {@link BlockEntity#clearRemoved()} are not called), and so there's no way to easily reschedule them for ticking.
     * <p>
     * Instead, for each chunk we keep a list of all block entities whose tick we skipped. If a chunk is loaded,
     * {@linkplain #onChunkTicketChanged(ServerLevel, long, int, int) we requeue all skipped ticks}.
     */
    private static final Map<ChunkReference, List<Token>> delayed = new HashMap<>();

    /**
     * Schedule a block entity to be ticked.
     *
     * @param token The token whose block entity should be ticked.
     */
    public static void schedule(Token token) {
        var world = token.owner.getLevel();
        if (world != null && !world.isClientSide && Token.STATE.compareAndSet(token, State.IDLE, State.SCHEDULED)) {
            toTick.add(token);
        }
    }

    public static void onChunkTicketChanged(ServerLevel level, long chunkPos, int oldLevel, int newLevel) {
        boolean oldLoaded = isLoaded(oldLevel), newLoaded = isLoaded(newLevel);
        if (!oldLoaded && newLoaded) {
            // If our chunk is becoming active, requeue all pending tokens.
            var delayedTokens = delayed.remove(new ChunkReference(level.dimension(), chunkPos));
            if (delayedTokens == null) return;

            for (var token : delayedTokens) {
                if (token.owner.isRemoved()) {
                    Token.STATE.set(token, State.IDLE);
                } else {
                    Token.STATE.set(token, State.SCHEDULED);
                    toTick.add(token);
                }
            }
        }
    }

    public static void onChunkUnload(LevelChunk chunk) {
        // If our chunk is fully unloaded, all block entities are about to be removed - we need to dequeue any delayed
        // tokens from the queue.
        var delayedTokens = delayed.remove(new ChunkReference(chunk.getLevel().dimension(), chunk.getPos().toLong()));
        if (delayedTokens == null) return;

        for (var token : delayedTokens) Token.STATE.set(token, State.IDLE);
    }

    public static void tick() {
        Token token;
        while ((token = toTick.poll()) != null) Token.STATE.set(token, tickToken(token));
    }

    private static State tickToken(Token token) {
        var blockEntity = token.owner;

        // If the block entity has been removed, then remove it from the queue.
        if (blockEntity.isRemoved()) return State.IDLE;

        var level = Objects.requireNonNull(blockEntity.getLevel(), "Block entity level cannot become null");
        var pos = blockEntity.getBlockPos();

        if (!level.isLoaded(pos)) {
            // The chunk is not properly loaded, as it to our delayed set.
            delayed.computeIfAbsent(new ChunkReference(level.dimension(), ChunkPos.asLong(pos)), x -> new ArrayList<>()).add(token);
            return State.UNLOADED;
        } else {
            // This should be impossible: either the block entity is at the above position, or it has been removed.
            var currentBlockEntity = level.getBlockEntity(pos);
            if (currentBlockEntity != blockEntity) {
                throw new IllegalStateException("Expected " + blockEntity + " at " + pos + ", got " + currentBlockEntity);
            }

            // Otherwise schedule a tick and remove it from the queue.
            level.scheduleTick(pos, blockEntity.getBlockState().getBlock(), 0);
            return State.IDLE;
        }
    }

    /**
     * An item which can be scheduled for future ticking.
     * <p>
     * This tracks whether the {@link BlockEntity} is queued or not, as this is more efficient than maintaining a set.
     * As such, it should be unique per {@link BlockEntity} instance to avoid it being queued multiple times.
     */
    public static class Token {
        static final AtomicReferenceFieldUpdater<Token, State> STATE = AtomicReferenceFieldUpdater.newUpdater(Token.class, State.class, "$state");

        final BlockEntity owner;

        /**
         * The current state of this token.
         */
        @Keep
        private volatile State $state = State.IDLE;

        public Token(BlockEntity owner) {
            this.owner = owner;
        }
    }

    /**
     * The possible states a {@link Token} can be in.
     * <p>
     * This effectively stores which (if any) queue the token is currently in, allowing us to skip scheduling if the
     * token is already enqueued.
     */
    private enum State {
        /**
         * The token is not on any queues.
         */
        IDLE,

        /**
         * The token is on the {@link #toTick} queue.
         */
        SCHEDULED,

        /**
         * The token is on the {@link #delayed} queue.
         */
        UNLOADED,
    }

    private record ChunkReference(ResourceKey<Level> level, Long position) {
        @Override
        public String toString() {
            return "ChunkReference(" + level + " at " + new ChunkPos(position) + ")";
        }
    }

    private static boolean isLoaded(int level) {
        return level <= ChunkLevel.byStatus(ChunkStatus.FULL);
    }
}
