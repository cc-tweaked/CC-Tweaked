/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.common.base.Predicate;
import com.google.common.collect.MapMaker;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public final class WorldUtil
{
    @SuppressWarnings( "Guava" )
    private static final Predicate<Entity> CAN_COLLIDE = x -> x != null && x.isAlive() && x.collides();

    private static final Map<World, Entity> entityCache = new MapMaker().weakKeys()
        .weakValues()
        .makeMap();

    public static boolean isLiquidBlock( World world, BlockPos pos )
    {
        if( !World.isInBuildLimit( pos ) )
        {
            return false;
        }
        return world.getBlockState( pos )
            .getMaterial()
            .isLiquid();
    }

    public static boolean isVecInside( VoxelShape shape, Vec3d vec )
    {
        if( shape.isEmpty() )
        {
            return false;
        }
        // AxisAlignedBB.contains, but without strict inequalities.
        Box bb = shape.getBoundingBox();
        return vec.x >= bb.minX && vec.x <= bb.maxX && vec.y >= bb.minY && vec.y <= bb.maxY && vec.z >= bb.minZ && vec.z <= bb.maxZ;
    }

    public static Pair<Entity, Vec3d> rayTraceEntities( World world, Vec3d vecStart, Vec3d vecDir, double distance )
    {
        Vec3d vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );

        // Raycast for blocks
        Entity collisionEntity = getEntity( world );
        collisionEntity.updatePosition( vecStart.x, vecStart.y, vecStart.z );
        RaycastContext context = new RaycastContext( vecStart,
            vecEnd,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            collisionEntity );
        HitResult result = world.raycast( context );
        if( result != null && result.getType() == HitResult.Type.BLOCK )
        {
            distance = vecStart.distanceTo( result.getPos() );
            vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );
        }

        // Check for entities
        float xStretch = Math.abs( vecDir.x ) > 0.25f ? 0.0f : 1.0f;
        float yStretch = Math.abs( vecDir.y ) > 0.25f ? 0.0f : 1.0f;
        float zStretch = Math.abs( vecDir.z ) > 0.25f ? 0.0f : 1.0f;
        Box bigBox = new Box( Math.min( vecStart.x, vecEnd.x ) - 0.375f * xStretch,
            Math.min( vecStart.y, vecEnd.y ) - 0.375f * yStretch,
            Math.min( vecStart.z, vecEnd.z ) - 0.375f * zStretch,
            Math.max( vecStart.x, vecEnd.x ) + 0.375f * xStretch,
            Math.max( vecStart.y, vecEnd.y ) + 0.375f * yStretch,
            Math.max( vecStart.z, vecEnd.z ) + 0.375f * zStretch );

        Entity closest = null;
        double closestDist = 99.0;
        List<Entity> list = world.getEntitiesByClass( Entity.class, bigBox, CAN_COLLIDE );
        for( Entity entity : list )
        {
            Box littleBox = entity.getBoundingBox();
            if( littleBox.contains( vecStart ) )
            {
                closest = entity;
                closestDist = 0.0f;
                continue;
            }

            Vec3d littleBoxResult = littleBox.raycast( vecStart, vecEnd )
                .orElse( null );
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

    private static synchronized Entity getEntity( World world )
    {
        // TODO: It'd be nice if we could avoid this. Maybe always use the turtle player (if it's available).
        Entity entity = entityCache.get( world );
        if( entity != null )
        {
            return entity;
        }

        entity = new ItemEntity( EntityType.ITEM, world )
        {
            @Nonnull
            @Override
            public EntityDimensions getDimensions( @Nonnull EntityPose pose )
            {
                return EntityDimensions.fixed( 0, 0 );
            }
        };

        entity.noClip = true;
        entity.calculateDimensions();
        entityCache.put( world, entity );
        return entity;
    }

    public static Vec3d getRayEnd( PlayerEntity player )
    {
        double reach = 5;
        Vec3d look = player.getRotationVector();
        return getRayStart( player ).add( look.x * reach, look.y * reach, look.z * reach );
    }

    public static Vec3d getRayStart( LivingEntity entity )
    {
        return entity.getCameraPosVec( 1 );
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
        dropItemStack( stack, world, new Vec3d( xPos, yPos, zPos ), xDir, yDir, zDir );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, Vec3d pos, double xDir, double yDir, double zDir )
    {
        ItemEntity item = new ItemEntity( world, pos.x, pos.y, pos.z, stack.copy() );
        item.setVelocity( xDir * 0.7 + world.getRandom()
                .nextFloat() * 0.2 - 0.1,
            yDir * 0.7 + world.getRandom()
                .nextFloat() * 0.2 - 0.1,
            zDir * 0.7 + world.getRandom()
                .nextFloat() * 0.2 - 0.1 );
        item.setToDefaultPickupDelay();
        world.spawnEntity( item );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, World world, Vec3d pos )
    {
        dropItemStack( stack, world, pos, 0.0, 0.0, 0.0 );
    }
}
