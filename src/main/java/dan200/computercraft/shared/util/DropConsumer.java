/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class DropConsumer {
    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static WeakReference<World> dropWorld;
    private static BlockPos dropPos;
    private static Box dropBounds;
    private static WeakReference<Entity> dropEntity;
    private DropConsumer() {
    }

    public static void set(Entity entity, Function<ItemStack, ItemStack> consumer) {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = new WeakReference<>(entity);
        dropWorld = new WeakReference<>(entity.world);
        dropPos = null;
        dropBounds = new Box(entity.getBlockPos()).expand(2, 2, 2);

        // entity.getScale().captureDrops( new ArrayList<>() );
    }

    public static void set(World world, BlockPos pos, Function<ItemStack, ItemStack> consumer) {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>(2);
        dropEntity = null;
        dropWorld = new WeakReference<>(world);
        dropBounds = new Box(pos).expand(2, 2, 2);
    }

    public static List<ItemStack> clear() {
        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    public static boolean onHarvestDrops(World world, BlockPos pos, ItemStack stack) {
        if (dropWorld != null && dropWorld.get() == world && dropPos != null && dropPos.equals(pos)) {
            handleDrops(stack);
            return true;
        }
        return false;
    }

    private static void handleDrops(ItemStack stack) {
        ItemStack remaining = dropConsumer.apply(stack);
        if (!remaining.isEmpty()) {
            remainingDrops.add(remaining);
        }
    }

    public static boolean onEntitySpawn(Entity entity) {
        // Capture any nearby item spawns
        if (dropWorld != null && dropWorld.get() == entity.getEntityWorld() && entity instanceof ItemEntity && dropBounds.contains(entity.getPos())) {
            handleDrops(((ItemEntity) entity).getStack());
            return true;
        }
        return false;
    }

    public static boolean onLivingDrops(Entity entity, ItemStack stack) {
        if (dropEntity != null && entity == dropEntity.get()) {
            handleDrops(stack);
            return true;
        }
        return false;
    }
}
