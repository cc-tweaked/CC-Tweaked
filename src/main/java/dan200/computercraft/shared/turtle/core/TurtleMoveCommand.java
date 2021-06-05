/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtleMoveCommand implements ITurtleCommand
{
    private final MoveDirection direction;

    public TurtleMoveCommand( MoveDirection direction )
    {
        this.direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get world direction from direction
        Direction direction = this.direction.toWorldDir( turtle );

        // Check if we can move
        World oldWorld = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.relative( direction );

        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, oldPosition, direction );
        TurtleCommandResult canEnterResult = canEnter( turtlePlayer, oldWorld, newPosition );
        if( !canEnterResult.isSuccess() )
        {
            return canEnterResult;
        }

        // Check existing block is air or replaceable
        BlockState state = oldWorld.getBlockState( newPosition );
        if( !oldWorld.isEmptyBlock( newPosition ) &&
            !WorldUtil.isLiquidBlock( oldWorld, newPosition ) &&
            !state.getMaterial().isReplaceable() )
        {
            return TurtleCommandResult.failure( "Movement obstructed" );
        }

        // Check there isn't anything in the way
        VoxelShape collision = state.getCollisionShape( oldWorld, oldPosition ).move(
            newPosition.getX(),
            newPosition.getY(),
            newPosition.getZ()
        );

        if( !oldWorld.isUnobstructed( null, collision ) )
        {
            if( !ComputerCraft.turtlesCanPush || this.direction == MoveDirection.UP || this.direction == MoveDirection.DOWN )
            {
                return TurtleCommandResult.failure( "Movement obstructed" );
            }

            // Check there is space for all the pushable entities to be pushed
            List<Entity> list = oldWorld.getEntitiesOfClass( Entity.class, getBox( collision ), x -> x != null && x.isAlive() && x.blocksBuilding );
            for( Entity entity : list )
            {
                AxisAlignedBB pushedBB = entity.getBoundingBox().move(
                    direction.getStepX(),
                    direction.getStepY(),
                    direction.getStepZ()
                );
                if( !oldWorld.isUnobstructed( null, VoxelShapes.create( pushedBB ) ) )
                {
                    return TurtleCommandResult.failure( "Movement obstructed" );
                }
            }
        }

        TurtleBlockEvent.Move moveEvent = new TurtleBlockEvent.Move( turtle, turtlePlayer, oldWorld, newPosition );
        if( MinecraftForge.EVENT_BUS.post( moveEvent ) )
        {
            return TurtleCommandResult.failure( moveEvent.getFailureMessage() );
        }

        // Check fuel level
        if( turtle.isFuelNeeded() && turtle.getFuelLevel() < 1 )
        {
            return TurtleCommandResult.failure( "Out of fuel" );
        }

        // Move
        if( !turtle.teleportTo( oldWorld, newPosition ) ) return TurtleCommandResult.failure( "Movement failed" );

        // Consume fuel
        turtle.consumeFuel( 1 );

        // Animate
        switch( this.direction )
        {
            case FORWARD:
            default:
                turtle.playAnimation( TurtleAnimation.MOVE_FORWARD );
                break;
            case BACK:
                turtle.playAnimation( TurtleAnimation.MOVE_BACK );
                break;
            case UP:
                turtle.playAnimation( TurtleAnimation.MOVE_UP );
                break;
            case DOWN:
                turtle.playAnimation( TurtleAnimation.MOVE_DOWN );
                break;
        }
        return TurtleCommandResult.success();
    }

    private static TurtleCommandResult canEnter( TurtlePlayer turtlePlayer, World world, BlockPos position )
    {
        if( World.isOutsideBuildHeight( position ) )
        {
            return TurtleCommandResult.failure( position.getY() < 0 ? "Too low to move" : "Too high to move" );
        }
        if( !World.isInWorldBounds( position ) ) return TurtleCommandResult.failure( "Cannot leave the world" );

        // Check spawn protection
        if( ComputerCraft.turtlesObeyBlockProtection && !TurtlePermissions.isBlockEnterable( world, position, turtlePlayer ) )
        {
            return TurtleCommandResult.failure( "Cannot enter protected area" );
        }

        if( !world.isAreaLoaded( position, 0 ) ) return TurtleCommandResult.failure( "Cannot leave loaded world" );
        if( !world.getWorldBorder().isWithinBounds( position ) )
        {
            return TurtleCommandResult.failure( "Cannot pass the world border" );
        }

        return TurtleCommandResult.success();
    }

    private static AxisAlignedBB getBox( VoxelShape shape )
    {
        return shape.isEmpty() ? EMPTY_BOX : shape.bounds();
    }

    private static final AxisAlignedBB EMPTY_BOX = new AxisAlignedBB( 0, 0, 0, 0, 0, 0 );
}
