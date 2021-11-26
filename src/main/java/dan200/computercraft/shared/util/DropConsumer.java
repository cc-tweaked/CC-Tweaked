/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class DropConsumer
{
    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static WeakReference<Level> dropWorld;
    private static BlockPos dropPos;
    private static AABB dropBounds;
    private static WeakReference<Entity> dropEntity;

    private DropConsumer()
    {
    }

    public static void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = new WeakReference<>( entity );
        dropWorld = new WeakReference<>( entity.level );
        dropPos = null;
        dropBounds = new AABB( entity.blockPosition() ).inflate( 2, 2, 2 );
    }

    public static void set( Level world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>( 2 );
        dropEntity = null;
        dropWorld = new WeakReference<>( world );
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

    public static boolean onHarvestDrops( Level world, BlockPos pos, ItemStack stack )
    {
        if( dropWorld != null && dropWorld.get() == world && dropPos != null && dropPos.equals( pos ) )
        {
            handleDrops( stack );
            return true;
        }
        return false;
    }

    private static void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() )
        {
            remainingDrops.add( remaining );
        }
    }

    public static boolean onEntitySpawn( Entity entity )
    {
        // Capture any nearby item spawns
        if( dropWorld != null && dropWorld.get() == entity.getCommandSenderWorld() && entity instanceof ItemEntity && dropBounds.contains( entity.position() ) )
        {
            handleDrops( ((ItemEntity) entity).getItem() );
            return true;
        }
        return false;
    }

    public static boolean onLivingDrops( Entity entity, ItemStack stack )
    {
        if( dropEntity != null && entity == dropEntity.get() )
        {
            handleDrops( stack );
            return true;
        }
        return false;
    }
}
