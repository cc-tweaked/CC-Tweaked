/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.base.Predicate;
import com.google.common.collect.MapMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public final class WorldUtil
{
    @SuppressWarnings( "Guava" )
    private static final Predicate<Entity> CAN_COLLIDE = x -> x != null && x.isAlive() && x.isPickable();

    private static final Map<Level, Entity> entityCache = new MapMaker().weakKeys().weakValues().makeMap();

    private static synchronized Entity getEntity( Level world )
    {
        // TODO: It'd be nice if we could avoid this. Maybe always use the turtle player (if it's available).
        Entity entity = entityCache.get( world );
        if( entity != null ) return entity;

        entity = new ItemEntity( EntityType.ITEM, world )
        {
            @Nonnull
            @Override
            public EntityDimensions getDimensions( @Nonnull Pose pose )
            {
                return EntityDimensions.fixed( 0, 0 );
            }
        };

        entity.noPhysics = true;
        entity.refreshDimensions();
        entityCache.put( world, entity );
        return entity;
    }

    public static boolean isLiquidBlock( Level world, BlockPos pos )
    {
        if( !world.isInWorldBounds( pos ) ) return false;
        return world.getBlockState( pos ).getMaterial().isLiquid();
    }

    public static boolean isVecInside( VoxelShape shape, Vec3 vec )
    {
        if( shape.isEmpty() ) return false;
        // AxisAlignedBB.contains, but without strict inequalities.
        AABB bb = shape.bounds();
        return vec.x >= bb.minX && vec.x <= bb.maxX && vec.y >= bb.minY && vec.y <= bb.maxY && vec.z >= bb.minZ && vec.z <= bb.maxZ;
    }

    public static Pair<Entity, Vec3> rayTraceEntities( Level world, Vec3 vecStart, Vec3 vecDir, double distance )
    {
        Vec3 vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );

        // Raycast for blocks
        Entity collisionEntity = getEntity( world );
        collisionEntity.setPos( vecStart.x, vecStart.y, vecStart.z );
        ClipContext context = new ClipContext( vecStart, vecEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, collisionEntity );
        HitResult result = world.clip( context );
        if( result != null && result.getType() == HitResult.Type.BLOCK )
        {
            distance = vecStart.distanceTo( result.getLocation() );
            vecEnd = vecStart.add( vecDir.x * distance, vecDir.y * distance, vecDir.z * distance );
        }

        // Check for entities
        float xStretch = Math.abs( vecDir.x ) > 0.25f ? 0.0f : 1.0f;
        float yStretch = Math.abs( vecDir.y ) > 0.25f ? 0.0f : 1.0f;
        float zStretch = Math.abs( vecDir.z ) > 0.25f ? 0.0f : 1.0f;
        AABB bigBox = new AABB(
            Math.min( vecStart.x, vecEnd.x ) - 0.375f * xStretch,
            Math.min( vecStart.y, vecEnd.y ) - 0.375f * yStretch,
            Math.min( vecStart.z, vecEnd.z ) - 0.375f * zStretch,
            Math.max( vecStart.x, vecEnd.x ) + 0.375f * xStretch,
            Math.max( vecStart.y, vecEnd.y ) + 0.375f * yStretch,
            Math.max( vecStart.z, vecEnd.z ) + 0.375f * zStretch
        );

        Entity closest = null;
        double closestDist = 99.0;
        List<Entity> list = world.getEntitiesOfClass( Entity.class, bigBox, CAN_COLLIDE );
        for( Entity entity : list )
        {
            AABB littleBox = entity.getBoundingBox();
            if( littleBox.contains( vecStart ) )
            {
                closest = entity;
                closestDist = 0.0f;
                continue;
            }

            Vec3 littleBoxResult = littleBox.clip( vecStart, vecEnd ).orElse( null );
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
            Vec3 closestPos = vecStart.add( vecDir.x * closestDist, vecDir.y * closestDist, vecDir.z * closestDist );
            return Pair.of( closest, closestPos );
        }
        return null;
    }

    public static Vec3 getRayStart( LivingEntity entity )
    {
        return entity.getEyePosition( 1 );
    }

    public static Vec3 getRayEnd( Player player )
    {
        double reach = player.getAttribute( ForgeMod.REACH_DISTANCE.get() ).getValue();
        Vec3 look = player.getLookAngle();
        return getRayStart( player ).add( look.x * reach, look.y * reach, look.z * reach );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, Level world, BlockPos pos )
    {
        dropItemStack( stack, world, pos, null );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, Level world, BlockPos pos, Direction direction )
    {
        double xDir;
        double yDir;
        double zDir;
        if( direction != null )
        {
            xDir = direction.getStepX();
            yDir = direction.getStepY();
            zDir = direction.getStepZ();
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
        dropItemStack( stack, world, new Vec3( xPos, yPos, zPos ), xDir, yDir, zDir );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, Level world, Vec3 pos )
    {
        dropItemStack( stack, world, pos, 0.0, 0.0, 0.0 );
    }

    public static void dropItemStack( @Nonnull ItemStack stack, Level world, Vec3 pos, double xDir, double yDir, double zDir )
    {
        ItemEntity item = new ItemEntity( world, pos.x, pos.y, pos.z, stack.copy() );
        item.setDeltaMovement(
            xDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1,
            yDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1,
            zDir * 0.7 + world.getRandom().nextFloat() * 0.2 - 0.1
        );
        item.setDefaultPickUpDelay();
        world.addFreshEntity( item );
    }
}
