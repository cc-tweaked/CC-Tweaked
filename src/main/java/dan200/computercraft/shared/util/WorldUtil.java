/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.common.base.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public final class WorldUtil
{
    @SuppressWarnings( "Guava" )
    private static final Predicate<Entity> CAN_COLLIDE = x -> x != null && x.isAlive() && x.doesCollide();

    public static boolean isLiquidBlock( World world, BlockPos pos )
    {
        if( !World.isValid( pos ) ) return false;
        BlockState state = world.getBlockState( pos );
        return !state.getFluidState().isEmpty();
    }

    public static boolean isVecInside( VoxelShape shape, Vec3d vec )
    {
        if( shape.isEmpty() ) return false;
        // AxisAlignedBB.contains, but without strict inequalities.
        BoundingBox bb = shape.getBoundingBox();
        return vec.x >= bb.minX && vec.x <= bb.maxX && vec.y >= bb.minY && vec.y <= bb.maxY && vec.z >= bb.minZ && vec.z <= bb.maxZ;
    }

    public static Pair<Entity, Vec3d> rayTraceEntities( World world, Vec3d vecStart, Vec3d vecDir, double distance )
    {
        Vec3d vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );

        // Raycast for blocks
        HitResult result = world.rayTrace( new RayTraceContext( vecStart, vecEnd, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, null ) );
        if( result != null && result.getType() == HitResult.Type.BLOCK )
        {
            distance = vecStart.distanceTo( result.getPos() );
            vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );
        }

        // Check for entities
        float xStretch = Math.abs( vecDir.x ) > 0.25f ? 0.0f : 1.0f;
        float yStretch = Math.abs( vecDir.y ) > 0.25f ? 0.0f : 1.0f;
        float zStretch = Math.abs( vecDir.z ) > 0.25f ? 0.0f : 1.0f;
        BoundingBox bigBox = new BoundingBox(
            Math.min( vecStart.x, vecEnd.x ) - 0.375f * xStretch,
            Math.min( vecStart.y, vecEnd.y ) - 0.375f * yStretch,
            Math.min( vecStart.z, vecEnd.z ) - 0.375f * zStretch,
            Math.max( vecStart.x, vecEnd.x ) + 0.375f * xStretch,
            Math.max( vecStart.y, vecEnd.y ) + 0.375f * yStretch,
            Math.max( vecStart.z, vecEnd.z ) + 0.375f * zStretch
        );

        Entity closest = null;
        double closestDist = 99.0;
        List<Entity> list = world.getEntitiesInBox( Entity.class, bigBox, CAN_COLLIDE );
        for( Entity entity : list )
        {
            BoundingBox littleBox = entity.getBoundingBox();

            if( littleBox.contains( vecStart ) )
            {
                closest = entity;
                closestDist = 0.0f;
                continue;
            }

            Vec3d littleBoxResult = littleBox.rayTrace( vecStart, vecEnd ).orElse( null );
            if( littleBoxResult != null )
            {
                double dist = vecStart.distanceTo( littleBoxResult );
                if( closest == null || dist <= closestDist )
                {
                    closest = entity;
                    closestDist = dist;
                }
            }
            else if( littleBox.intersects( bigBox ) )
            {
                if( closest == null )
                {
                    closest = entity;
                    closestDist = distance;
                }
            }
        }
        if( closest != null && closestDist <= distance )
        {
            Vec3d closestPos = vecStart.add( vecDir.x * closestDist, vecDir.y * closestDist, vecDir.z * closestDist );
            return Pair.of( closest, closestPos );
        }
        return null;
    }

    public static Vec3d getRayStart( LivingEntity entity )
    {
        return entity.getCameraPosVec( 1 );
    }

    public static Vec3d getRayEnd( PlayerEntity player )
    {
        double reach = 5; // TODO: player.getAttributeInstance( PlayerEntity.REACH_DISTANCE ).getAttributeValue();
        Vec3d look = player.getRotationVec( 1 );
        return getRayStart( player ).add( look.x * reach, look.y * reach, look.z * reach );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, BlockPos pos )
    {
        dropItemStack( stack, world, pos, null );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, BlockPos pos, Direction direction )
    {
        double xDir;
        double yDir;
        double zDir;
        if( direction != null )
        {
            xDir = direction.getOffsetX();
            yDir = direction.getOffsetY();
            zDir = direction.getOffsetZ();
        }
        else
        {
            xDir = 0.0;
            yDir = 0.0;
            zDir = 0.0;
        }

        double xPos = pos.getX() + 0.5 + xDir * 0.4;
        double yPos = pos.getY() + 0.5 + yDir * 0.4;
        double zPos = pos.getZ() + 0.5 + zDir * 0.4;
        dropItemStack( stack, world, xPos, yPos, zPos, xDir, yDir, zDir );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, double xPos, double yPos, double zPos )
    {
        dropItemStack( stack, world, xPos, yPos, zPos, 0.0, 0.0, 0.0 );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, double xPos, double yPos, double zPos, double xDir, double yDir, double zDir )
    {
        ItemEntity item = new ItemEntity( world, xPos, yPos, zPos, stack.copy() );
        item.setVelocity(
            xDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1,
            yDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1,
            zDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1
        );
        item.resetPickupDelay();
        world.spawnEntity( item );
    }
}
