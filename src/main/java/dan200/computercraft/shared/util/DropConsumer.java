/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class DropConsumer
{
    private DropConsumer()
    {
    }

    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static World dropWorld;
    private static AxisAlignedBB dropBounds;
    private static Entity dropEntity;

    public static void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = entity;
        dropWorld = entity.level;
        dropBounds = new AxisAlignedBB( entity.blockPosition() ).inflate( 2, 2, 2 );
    }

    public static void set( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>( 2 );
        dropEntity = null;
        dropWorld = world;
        dropBounds = new AxisAlignedBB( pos ).inflate( 2, 2, 2 );
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

    public static void clearAndDrop( World world, BlockPos pos, Direction direction )
    {
        List<ItemStack> remainingDrops = clear();
        for( ItemStack remaining : remainingDrops ) WorldUtil.dropItemStack( remaining, world, pos, direction );
    }

    private static void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    @SubscribeEvent( priority = EventPriority.HIGHEST )
    public static void onEntitySpawn( EntityJoinWorldEvent event )
    {
        // Capture any nearby item spawns
        if( dropWorld == event.getWorld() && event.getEntity() instanceof ItemEntity
            && dropBounds.contains( event.getEntity().position() ) )
        {
            handleDrops( ((ItemEntity) event.getEntity()).getItem() );
            event.setCanceled( true );
        }
    }

    @SubscribeEvent( priority = EventPriority.LOW )
    public static void onLivingDrops( LivingDropsEvent drops )
    {
        if( dropEntity == null || drops.getEntity() != dropEntity ) return;

        for( ItemEntity drop : drops.getDrops() ) handleDrops( drop.getItem() );
        drops.getDrops().clear();
        drops.setCanceled( true );
    }
}
