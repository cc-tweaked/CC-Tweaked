/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle.event;

import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleVerb;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * A general event for when a turtle interacts with a block or region.
 *
 * You should generally listen to one of the sub-events instead, cancelling them where
 * appropriate.
 *
 * Note that you are not guaranteed to receive this event, if it has been cancelled by other
 * mechanisms, such as block protection systems.
 *
 * Be aware that some events (such as {@link TurtleInventoryEvent}) do not necessarily interact
 * with a block, simply objects within that block space.
 */
public abstract class TurtleBlockEvent extends TurtlePlayerEvent
{
    private final World world;
    private final BlockPos pos;

    protected TurtleBlockEvent( @Nonnull ITurtleAccess turtle, @Nonnull TurtleAction action, @Nonnull FakePlayer player, @Nonnull World world, @Nonnull BlockPos pos )
    {
        super( turtle, action, player );

        Objects.requireNonNull( world, "world cannot be null" );
        Objects.requireNonNull( pos, "pos cannot be null" );
        this.world = world;
        this.pos = pos;
    }

    /**
     * Get the world the turtle is interacting in.
     *
     * @return The world the turtle is interacting in.
     */
    public World getWorld()
    {
        return world;
    }

    /**
     * Get the position the turtle is interacting with. Note that this is different
     * to {@link ITurtleAccess#getPosition()}.
     *
     * @return The position the turtle is interacting with.
     */
    public BlockPos getPos()
    {
        return pos;
    }

    /**
     * Fired when a turtle attempts to dig a block.
     *
     * This must be fired by {@link ITurtleUpgrade#useTool(ITurtleAccess, TurtleSide, TurtleVerb, Direction)},
     * as the base {@code turtle.dig()} command does not fire it.
     *
     * Note that such commands should also fire {@link BlockEvent.BreakEvent}, so you do not need to listen to both.
     *
     * @see TurtleAction#DIG
     */
    public static class Dig extends TurtleBlockEvent
    {
        private final BlockState block;
        private final ITurtleUpgrade upgrade;
        private final TurtleSide side;

        public Dig( @Nonnull ITurtleAccess turtle, @Nonnull FakePlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState block, @Nonnull ITurtleUpgrade upgrade, @Nonnull TurtleSide side )
        {
            super( turtle, TurtleAction.DIG, player, world, pos );

            Objects.requireNonNull( block, "block cannot be null" );
            Objects.requireNonNull( upgrade, "upgrade cannot be null" );
            Objects.requireNonNull( side, "side cannot be null" );
            this.block = block;
            this.upgrade = upgrade;
            this.side = side;
        }

        /**
         * Get the block which is about to be broken.
         *
         * @return The block which is going to be broken.
         */
        @Nonnull
        public BlockState getBlock()
        {
            return block;
        }

        /**
         * Get the upgrade doing the digging.
         *
         * @return The upgrade doing the digging.
         */
        @Nonnull
        public ITurtleUpgrade getUpgrade()
        {
            return upgrade;
        }

        /**
         * Get the side the upgrade doing the digging is on.
         *
         * @return The upgrade's side.
         */
        @Nonnull
        public TurtleSide getSide()
        {
            return side;
        }
    }

    /**
     * Fired when a turtle attempts to move into a block.
     *
     * @see TurtleAction#MOVE
     */
    public static class Move extends TurtleBlockEvent
    {
        public Move( @Nonnull ITurtleAccess turtle, @Nonnull FakePlayer player, @Nonnull World world, @Nonnull BlockPos pos )
        {
            super( turtle, TurtleAction.MOVE, player, world, pos );
        }
    }

    /**
     * Fired when a turtle attempts to place a block in the world.
     *
     * @see TurtleAction#PLACE
     */
    public static class Place extends TurtleBlockEvent
    {
        private final ItemStack stack;

        public Place( @Nonnull ITurtleAccess turtle, @Nonnull FakePlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull ItemStack stack )
        {
            super( turtle, TurtleAction.PLACE, player, world, pos );

            Objects.requireNonNull( stack, "stack cannot be null" );
            this.stack = stack;
        }

        /**
         * Get the item stack that will be placed. This should not be modified.
         *
         * @return The item stack to be placed.
         */
        @Nonnull
        public ItemStack getStack()
        {
            return stack;
        }
    }

    /**
     * Fired when a turtle gathers data on a block in world.
     *
     * You may prevent blocks being inspected, or add additional information to the result.
     *
     * @see TurtleAction#INSPECT
     */
    public static class Inspect extends TurtleBlockEvent
    {
        private final BlockState state;
        private final Map<String, Object> data;

        public Inspect( @Nonnull ITurtleAccess turtle, @Nonnull FakePlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Map<String, Object> data )
        {
            super( turtle, TurtleAction.INSPECT, player, world, pos );

            Objects.requireNonNull( state, "state cannot be null" );
            Objects.requireNonNull( data, "data cannot be null" );
            this.data = data;
            this.state = state;
        }

        /**
         * Get the block state which is being inspected.
         *
         * @return The inspected block state.
         */
        @Nonnull
        public BlockState getState()
        {
            return state;
        }

        /**
         * Get the "inspection data" from this block, which will be returned to the user.
         *
         * @return This block's inspection data.
         */
        @Nonnull
        public Map<String, Object> getData()
        {
            return data;
        }

        /**
         * Add new information to the inspection result. Note this will override fields with the same name.
         *
         * @param newData The data to add. Note all values should be convertible to Lua (see
         *                {@link MethodResult#of(Object)}).
         */
        public void addData( @Nonnull Map<String, ?> newData )
        {
            Objects.requireNonNull( newData, "newData cannot be null" );
            data.putAll( newData );
        }
    }
}
