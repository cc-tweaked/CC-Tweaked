// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class WorldUtil {
    public static boolean isLiquidBlock(Level world, BlockPos pos) {
        if (!world.isInWorldBounds(pos)) return false;
        return world.getBlockState(pos).liquid();
    }

    public static boolean isVecInside(VoxelShape shape, Vec3 vec) {
        if (shape.isEmpty()) return false;
        // AxisAlignedBB.contains, but without strict inequalities.
        var bb = shape.bounds();
        return vec.x >= bb.minX && vec.x <= bb.maxX && vec.y >= bb.minY && vec.y <= bb.maxY && vec.z >= bb.minZ && vec.z <= bb.maxZ;
    }

    public static HitResult clip(Level world, Vec3 from, Vec3 direction, double distance, @Nullable Entity source) {
        var to = from.add(direction.x * distance, direction.y * distance, direction.z * distance);
        return clip(world, from, to, source);
    }

    public static HitResult clip(Level world, Vec3 from, Vec3 to, @Nullable Entity source) {
        var context = source == null
            ? new ContextlessClipContext(world, from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE)
            : new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source);

        var blockHit = world.clip(context);
        var distance = blockHit.getType() == HitResult.Type.MISS
            ? from.distanceToSqr(to) : blockHit.getLocation().distanceToSqr(from);
        var entityHit = getEntityHitResult(world, from, to, new AABB(from, to).inflate(1), distance, source);

        return entityHit == null ? blockHit : entityHit;
    }

    /**
     * Perform a ray trace to the nearest entity. Derived from the various methods in {@link ProjectileUtil}.
     *
     * @param level      The current level.
     * @param from       The start point of the ray trace.
     * @param to         The end point of the ray trace.
     * @param bounds     The range the entities should be within.
     * @param distanceSq The maximum distance an entity can be away from the start vector.
     * @param source     An optional entity to ignore, which typically will be the entity performing the ray trace.
     * @return The found entity, or {@code null}.
     */
    private static @Nullable EntityHitResult getEntityHitResult(
        Level level, Vec3 from, Vec3 to, AABB bounds, double distanceSq, @Nullable Entity source
    ) {
        // If the distance is empty, we'll never collide anyway!
        if (distanceSq <= 0) return null;

        var bestDistance = distanceSq;
        Entity bestEntity = null;
        Vec3 bestHit = null;

        for (var entity : level.getEntities(source, bounds, WorldUtil::canCollide)) {
            var aabb = entity.getBoundingBox().inflate(entity.getPickRadius());

            // clip doesn't work when inside the entity. Just assume we've got a perfect match and break.
            if (aabb.contains(from)) {
                bestHit = from;
                bestEntity = entity;
                break;
            }

            var clip = aabb.clip(from, to);
            if (clip.isEmpty()) continue;

            var hit = clip.get();
            var newDistance = from.distanceToSqr(hit);
            if (newDistance < bestDistance) {
                bestEntity = entity;
                bestHit = hit;
                bestDistance = newDistance;
            }
        }

        return bestEntity == null ? null : new EntityHitResult(bestEntity, bestHit);
    }

    private static boolean canCollide(Entity entity) {
        return entity != null && entity.isAlive() && entity.isPickable();
    }

    public static Vec3 getRayStart(Player entity) {
        return entity.getEyePosition();
    }

    public static Vec3 getRayEnd(Player player) {
        var reach = PlatformHelper.get().getReachDistance(player);
        var look = player.getLookAngle();
        return getRayStart(player).add(look.x * reach, look.y * reach, look.z * reach);
    }

    private static final double DROP_SPEED = 0.0172275 * 6;

    /**
     * Drop an item stack into the world from a block.
     * <p>
     * This behaves similarly to {@link DefaultDispenseItemBehavior#spawnItem(Level, ItemStack, int, Direction, Position)},
     * though supports a {@code null} direction (in which case the item will have no velocity) and produces a slightly
     * different arc.
     *
     * @param level     The level to drop the item in.
     * @param pos       The position to drop the stack from.
     * @param direction The direction to drop in, or {@code null}.
     * @param stack     The stack to drop.
     */
    public static void dropItemStack(Level level, BlockPos pos, @Nullable Direction direction, ItemStack stack) {
        double xDir;
        double yDir;
        double zDir;
        if (direction != null) {
            xDir = direction.getStepX();
            yDir = direction.getStepY();
            zDir = direction.getStepZ();
        } else {
            xDir = 0.0;
            yDir = 0.0;
            zDir = 0.0;
        }

        var xPos = pos.getX() + 0.5 + xDir * 0.7;
        var yPos = pos.getY() + 0.5 + yDir * 0.7;
        var zPos = pos.getZ() + 0.5 + zDir * 0.7;

        var item = new ItemEntity(level, xPos, yPos, zPos, stack.copy());
        var baseSpeed = level.random.nextDouble() * 0.1 + 0.2;
        item.setDeltaMovement(
            level.random.triangle(xDir * baseSpeed, DROP_SPEED),
            // Vanilla ignores the yDir and does a constant 0.2, but that gives the item a higher arc than we want.
            level.random.triangle(yDir * baseSpeed, DROP_SPEED),
            level.random.triangle(zDir * baseSpeed, DROP_SPEED)
        );
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /**
     * A custom {@link ClipContext} which allows an empty entity.
     * <p>
     * This isn't needed on Forge, but is useful on Fabric.
     */
    private static class ContextlessClipContext extends ClipContext {
        private final Block block;

        ContextlessClipContext(Level level, Vec3 from, Vec3 to, Block block, Fluid fluid) {
            super(from, to, block, fluid, new ItemEntity(EntityType.ITEM, level));
            this.block = block;
        }

        @Override
        public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
            return block.get(state, level, pos, CollisionContext.empty());
        }
    }
}
