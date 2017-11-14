/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
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
        EnumFacing direction = m_direction.toWorldDir( turtle );

        // Check if we can move
        World oldWorld = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = WorldUtil.moveCoords( oldPosition, direction );

        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleCommandResult canEnterResult = canEnter( turtlePlayer, oldWorld, newPosition );
        if( !canEnterResult.isSuccess() )
        {
            return canEnterResult;
        }

        // Check existing block is air or replaceable
        Block block = oldWorld.getBlockState( newPosition ).getBlock();
        if( block != null &&
            !oldWorld.isAirBlock( newPosition ) &&
            !WorldUtil.isLiquidBlock( oldWorld, newPosition ) &&
            !block.isReplaceable( oldWorld, newPosition ) )
        {
            return TurtleCommandResult.failure( "Movement obstructed" );
        }

        // Check there isn't anything in the way
        AxisAlignedBB aabb = ((TurtleBrain)turtle).getOwner().getBounds();
        aabb = aabb.offset(
            newPosition.getX(),
            newPosition.getY(),
            newPosition.getZ()
        );
        if( !oldWorld.checkNoEntityCollision( aabb ) )
        {
            if( ComputerCraft.turtlesCanPush && m_direction != MoveDirection.Up && m_direction != MoveDirection.Down )
            {
                // Check there is space for all the pushable entities to be pushed
                List<Entity> list = oldWorld.getEntitiesWithinAABBExcludingEntity( null, aabb );
                for( Entity entity : list )
                {
                    if( !entity.isDead && entity.preventEntitySpawning )
                    {
                        AxisAlignedBB entityBB = entity.getEntityBoundingBox();
                        if( entityBB == null )
                        {
                            entityBB = entity.getCollisionBoundingBox();
                        }
                        if( entityBB != null )
                        {
                            AxisAlignedBB pushedBB = entityBB.offset(
                                direction.getFrontOffsetX(),
                                direction.getFrontOffsetY(),
                                direction.getFrontOffsetZ()
                            );
                            if( !oldWorld.getCollisionBoxes( null, pushedBB ).isEmpty() )
                            {
                                return TurtleCommandResult.failure( "Movement obstructed" );
                            }
                        }
                    }
                }
            }
            else
            {
                return TurtleCommandResult.failure( "Movement obstructed" );
            }
        }

        // Check fuel level
        if( turtle.isFuelNeeded() && turtle.getFuelLevel() < 1 )
        {
            return TurtleCommandResult.failure( "Out of fuel" );
        }

        // Move
        if( turtle.teleportTo( oldWorld, newPosition ) )
        {
            // Consume fuel
            turtle.consumeFuel( 1 );

            // Animate
            switch( m_direction )
            {
                case Forward:
                default:
                {
                    turtle.playAnimation( TurtleAnimation.MoveForward );
                    break;
                }
                case Back:
                {
                    turtle.playAnimation( TurtleAnimation.MoveBack );
                    break;
                }
                case Up:
                {
                    turtle.playAnimation( TurtleAnimation.MoveUp );
                    break;
                }
                case Down:
                {
                    turtle.playAnimation( TurtleAnimation.MoveDown );
                    break;
                }
            }
            return TurtleCommandResult.success();
        }
        else
        {
            return TurtleCommandResult.failure( "Movement failed" );
        }
    }

    private TurtleCommandResult canEnter( TurtlePlayer turtlePlayer, World world, BlockPos position )
    {
        if( position.getY() < 0 )
        {
            return TurtleCommandResult.failure( "Too low to move" );
        }
        else if( position.getY() > world.getHeight() - 1 )
        {
            return TurtleCommandResult.failure( "Too high to move" );
        }
        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            if( !ComputerCraft.isBlockEnterable( world, position, turtlePlayer ) )
            {
                return TurtleCommandResult.failure( "Cannot enter protected area" );
            }
        }
        if( !world.isBlockLoaded( position ) )
        {
            return TurtleCommandResult.failure( "Cannot leave loaded world" );
        }
        return TurtleCommandResult.success();
    }
}
