// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public final class DropConsumer {
    private DropConsumer() {
    }

    private static @Nullable Function<ItemStack, ItemStack> dropConsumer;
    private static @Nullable List<ItemStack> remainingDrops;
    private static @Nullable Level dropWorld;
    private static @Nullable AABB dropBounds;
    private static @Nullable Entity dropEntity;

    public static void set(Entity entity, Function<ItemStack, ItemStack> consumer) {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = entity;
        dropWorld = entity.level();
        dropBounds = new AABB(entity.blockPosition()).inflate(2, 2, 2);
    }

    public static void set(Level world, BlockPos pos, Function<ItemStack, ItemStack> consumer) {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>(2);
        dropEntity = null;
        dropWorld = world;
        dropBounds = new AABB(pos).inflate(2, 2, 2);
    }

    public static List<ItemStack> clear() {
        var remainingStacks = remainingDrops;
        if (remainingStacks == null) throw new IllegalStateException("Not currently capturing");

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    public static void clearAndDrop(Level world, BlockPos pos, @Nullable Direction direction) {
        var remainingDrops = clear();
        for (var remaining : remainingDrops) WorldUtil.dropItemStack(world, pos, direction, remaining);
    }

    private static void handleDrops(ItemStack stack) {
        var remaining = assertNonNull(dropConsumer).apply(stack);
        if (!remaining.isEmpty()) assertNonNull(remainingDrops).add(remaining);
    }

    public static boolean onEntitySpawn(Entity entity) {
        // Capture any nearby item spawns
        if (dropWorld == entity.level() && entity instanceof ItemEntity
            && assertNonNull(dropBounds).contains(entity.position())) {
            handleDrops(((ItemEntity) entity).getItem());
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
