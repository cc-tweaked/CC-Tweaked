/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle;

import com.google.common.eventbus.Subscribe;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleRefuelEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import javax.annotation.Nonnull;

public final class FurnaceRefuelHandler implements TurtleRefuelEvent.Handler
{
    public static final FurnaceRefuelHandler INSTANCE = new FurnaceRefuelHandler();

    private FurnaceRefuelHandler()
    {
    }

    @Override
    public int refuel( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack currentStack, int slot, int limit )
    {
        int fuelSpaceLeft = turtle.getFuelLimit() - turtle.getFuelLevel();
        int fuelPerItem = getFuelPerItem( turtle.getItemHandler().getStack( slot ) );
        int fuelItemLimit = (int) Math.ceil( fuelSpaceLeft / (double) fuelPerItem );
        if( limit > fuelItemLimit ) limit = fuelItemLimit;

        ItemStack stack = turtle.getItemHandler().take( slot, limit, ItemStack.EMPTY, false );
        int fuelToGive = fuelPerItem * stack.getCount();
        // Store the replacement item in the inventory
        Item replacementStack = stack.getItem().getCraftingRemainingItem();
        if( replacementStack != null )
        {
            ItemStack remainder = InventoryUtil.storeItems( new ItemStack( replacementStack ), turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getLevel(), turtle.getPosition(), turtle.getDirection().getOpposite() );
            }
        }

        return fuelToGive;
    }

    private static int getFuelPerItem( @Nonnull ItemStack stack )
    {
        int burnTime = FurnaceBlockEntity.getFuel().getOrDefault( stack.getItem(), 0 );
        return (burnTime * 5) / 100;
    }

    @Subscribe
    public static void onTurtleRefuel( TurtleRefuelEvent event )
    {
        if( event.getHandler() == null && getFuelPerItem( event.getStack() ) > 0 ) event.setHandler( INSTANCE );
    }
}
