/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DropConsumer
{
    private DropConsumer()
    {
    }

    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static Level dropWorld;
    private static BlockPos dropPos;
    private static AABB dropBounds;
    private static Entity dropEntity;

    public static void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = entity;
        dropWorld = entity.level;
        dropPos = null;
        dropBounds = new AABB( entity.blockPosition() ).inflate( 2, 2, 2 );
    }

    public static void set( Level world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>( 2 );
        dropEntity = null;
        dropWorld = world;
        dropBounds = new AABB( pos ).inflate( 2, 2, 2 );
    }

    public static List<ItemStack> clear()
    {
        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    public static void clearAndDrop( Level world, BlockPos pos, Direction direction )
    {
        List<ItemStack> remainingDrops = clear();
        for( ItemStack remaining : remainingDrops ) WorldUtil.dropItemStack( remaining, world, pos, direction );
    }

    private static void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    public static boolean onEntitySpawn( Entity entity )
    {
        // Capture any nearby item spawns
        if( dropWorld != null && dropWorld == entity.getCommandSenderWorld() && entity instanceof ItemEntity
            && dropBounds.contains( entity.position() ) )
        {
            handleDrops( ((ItemEntity) entity).getItem() );
            return true;
        }
        return false;
    }

    public static boolean onLivingDrops( Entity entity, ItemStack stack )
    {
        if( dropEntity == null || entity != dropEntity ) return false;

        handleDrops( stack );
        return true;
    }

    public static boolean onHarvestDrops( Level world, BlockPos pos, ItemStack stack )
    {
        if( dropWorld != null && dropWorld == world && dropPos != null && dropPos.equals( pos ) )
        {
            handleDrops( stack );
            return true;
        }
        return false;
    }
}
