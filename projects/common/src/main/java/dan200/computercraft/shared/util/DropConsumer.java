// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public final class DropConsumer {
    private DropConsumer() {
    }

    private static @Nullable List<ItemStack> drops;
    private static @Nullable Level dropWorld;
    private static @Nullable AABB dropBounds;
    private static @Nullable Entity dropEntity;

    public static void set(Entity entity) {
        drops = new ArrayList<>();
        dropEntity = entity;
        dropWorld = entity.level();
        dropBounds = new AABB(entity.blockPosition()).inflate(2, 2, 2);
    }

    public static void set(Level world, BlockPos pos) {
        drops = new ArrayList<>(2);
        dropEntity = null;
        dropWorld = world;
        dropBounds = new AABB(pos).inflate(2, 2, 2);
    }

    public static List<ItemStack> stop() {
        var remainingStacks = drops;
        if (remainingStacks == null) throw new IllegalStateException("Not currently capturing");

        drops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    private static void handleDrops(ItemStack stack) {
        assertNonNull(drops).add(stack);
    }

    public static boolean onEntitySpawn(Entity entity) {
        // Capture any nearby item spawns
        if (dropWorld == entity.level() && entity instanceof ItemEntity item
            && assertNonNull(dropBounds).contains(entity.position())) {
            handleDrops(item.getItem());
            return true;
        }

        return false;
    }

    public static boolean onLivingDrop(Entity entity, ItemStack stack) {
        if (entity != dropEntity) return false;

        handleDrops(stack);
        return true;
    }
}
