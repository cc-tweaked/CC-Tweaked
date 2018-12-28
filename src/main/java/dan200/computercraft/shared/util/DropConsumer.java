/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DropConsumer
{
    private static final DropConsumer instance = new DropConsumer();

    public static DropConsumer instance()
    {
        return instance;
    }

    private Function<ItemStack, ItemStack> dropConsumer;
    private List<ItemStack> remainingDrops;
    private WeakReference<World> dropWorld;
    private BlockPos dropPos;
    private AxisAlignedBB dropBounds;
    private WeakReference<Entity> dropEntity;

    public void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = new WeakReference<>( entity );
        dropWorld = new WeakReference<>( entity.world );
        dropPos = null;
        dropBounds = new AxisAlignedBB( entity.getPosition() ).grow( 2, 2, 2 );

        entity.captureDrops = true;
    }

    public void set( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = null;
        dropWorld = new WeakReference<>( world );
        dropPos = pos;
        dropBounds = new AxisAlignedBB( pos ).grow( 2, 2, 2 );
    }

    public List<ItemStack> clear()
    {
        if( dropEntity != null )
        {
            Entity entity = dropEntity.get();
            if( entity != null )
            {
                entity.captureDrops = false;
                if( entity.capturedDrops != null )
                {
                    for( EntityItem entityItem : entity.capturedDrops ) handleDrops( entityItem.getItem() );
                    entity.capturedDrops.clear();
                }
            }
        }

        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropPos = null;
        dropBounds = null;

        return remainingStacks;
    }

    private void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    @SubscribeEvent( priority = EventPriority.LOWEST )
    public void onEntityLivingDrops( LivingDropsEvent event )
    {
        // Capture any mob drops for the current entity
        if( dropEntity != null && event.getEntity() == dropEntity.get() )
        {
            List<EntityItem> drops = event.getDrops();
            for( EntityItem entityItem : drops ) handleDrops( entityItem.getItem() );
            drops.clear();
        }
    }

    @SubscribeEvent( priority = EventPriority.LOWEST )
    public void onHarvestDrops( BlockEvent.HarvestDropsEvent event )
    {
        // Capture block drops for the current entity
        if( dropWorld != null && dropWorld.get() == event.getWorld()
            && dropPos != null && dropPos.equals( event.getPos() ) )
        {
            for( ItemStack item : event.getDrops() )
            {
                if( event.getWorld().rand.nextFloat() < event.getDropChance() ) handleDrops( item );
            }
            event.getDrops().clear();
        }
    }

    @SubscribeEvent( priority = EventPriority.LOWEST )
    public void onEntitySpawn( EntityJoinWorldEvent event )
    {
        // Capture any nearby item spawns
        if( dropWorld != null && dropWorld.get() == event.getWorld() && event.getEntity() instanceof EntityItem
            && dropBounds.contains( event.getEntity().getPositionVector() ) )
        {
            handleDrops( ((EntityItem) event.getEntity()).getItem() );
            event.setCanceled( true );
        }
    }
}
