/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtleMoveCommand implements ITurtleCommand
{
    private final MoveDirection m_direction;

    public TurtleMoveCommand( MoveDirection direction )
    {
        m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get world direction from direction
        Direction direction = m_direction.toWorldDir( turtle );

        // Check if we can move
        World oldWorld = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );

        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleCommandResult canEnterResult = canEnter( turtlePlayer, oldWorld, newPosition );
        if( !canEnterResult.isSuccess() )
        {
            return canEnterResult;
        }

        // Check existing block is air or replaceable
        BlockState state = oldWorld.getBlockState( newPosition );
        Block block = state.getBlock();
        if( block != null &&
            !oldWorld.isAir( newPosition ) &&
            !WorldUtil.isLiquidBlock( oldWorld, newPosition ) &&
            !state.getMaterial().isReplaceable() )
        {
            return TurtleCommandResult.failure( "Movement obstructed" );
        }

        // Check there isn't anything in the way
        BoundingBox aabb = getBox( state.getCollisionShape( oldWorld, oldPosition ) );
        aabb = aabb.offset(
            newPosition.getX(),
            newPosition.getY(),
            newPosition.getZ()
        );
        if( !oldWorld.doesNotCollide( aabb ) )
        {
            if( !ComputerCraft.turtlesCanPush || m_direction == MoveDirection.Up || m_direction == MoveDirection.Down )
            {
                return TurtleCommandResult.failure( "Movement obstructed" );
            }

            // Check there is space for all the pushable entities to be pushed
            List<Entity> list = oldWorld.getEntities( (Entity) null, aabb, EntityPredicates.VALID_ENTITY );
            for( Entity entity : list )
            {
                BoundingBox entityBB = entity.getBoundingBox();
                if( entityBB == null ) continue;

                BoundingBox pushedBB = entityBB.offset(
                    direction.getOffsetX(),
                    direction.getOffsetY(),
                    direction.getOffsetZ()
                );
                if( oldWorld.doesNotCollide( pushedBB ) )
                {
                    return TurtleCommandResult.failure( "Movement obstructed" );
                }
            }
        }

        TurtleBlockEvent.Move moveEvent = new TurtleBlockEvent.Move( turtle, turtlePlayer, oldWorld, newPosition );
        if( TurtleEvent.post( moveEvent ) )
        {
            return TurtleCommandResult.failure( moveEvent.getFailureMessage() );
        }

        // Check fuel level
        if( turtle.isFuelNeeded() && turtle.getFuelLevel() < 1 )
        {
            return TurtleCommandResult.failure( "Out of fuel" );
        }

        // Move
        if( !turtle.teleportTo( oldWorld, newPosition ) )
        {
            return TurtleCommandResult.failure( "Movement failed" );
        }

        // Consume fuel
        turtle.consumeFuel( 1 );

        // Animate
        switch( m_direction )
        {
            case Forward:
            default:
                turtle.playAnimation( TurtleAnimation.MoveForward );
                break;
            case Back:
                turtle.playAnimation( TurtleAnimation.MoveBack );
                break;
            case Up:
                turtle.playAnimation( TurtleAnimation.MoveUp );
                break;
            case Down:
                turtle.playAnimation( TurtleAnimation.MoveDown );
                break;
        }
        return TurtleCommandResult.success();
    }

    private static TurtleCommandResult canEnter( TurtlePlayer turtlePlayer, World world, BlockPos position )
    {
        if( World.isHeightInvalid( position ) )
        {
            return TurtleCommandResult.failure( position.getY() < 0 ? "Too low to move" : "Too high to move" );
        }
        if( !World.isValid( position ) ) return TurtleCommandResult.failure( "Cannot leave the world" );

        // Check spawn protection
        if( ComputerCraft.turtlesObeyBlockProtection && !TurtlePermissions.isBlockEnterable( world, position, turtlePlayer ) )
        {
            return TurtleCommandResult.failure( "Cannot enter protected area" );
        }

        if( !world.isBlockLoaded( position ) ) return TurtleCommandResult.failure( "Cannot leave loaded world" );
        if( !world.getWorldBorder().contains( position ) )
        {
            return TurtleCommandResult.failure( "Cannot pass the world border" );
        }

        return TurtleCommandResult.success();
    }

    private static BoundingBox getBox( VoxelShape shape )
    {
        return shape.isEmpty() ? EMPTY_BOX : shape.getBoundingBox();
    }

    private static final BoundingBox EMPTY_BOX = new BoundingBox( 0, 0, 0, 0, 0, 0 );
}
