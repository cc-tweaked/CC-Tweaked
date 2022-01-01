/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleRefuelEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class FurnaceRefuelHandler implements TurtleRefuelEvent.Handler
{
    private static final FurnaceRefuelHandler INSTANCE = new FurnaceRefuelHandler();

    private FurnaceRefuelHandler()
    {
    }

    @Override
    public int refuel( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack currentStack, int slot, int limit )
    {
        int fuelSpaceLeft = turtle.getFuelLimit() - turtle.getFuelLevel();
        int fuelPerItem = getFuelPerItem( turtle.getItemHandler().getStackInSlot( slot ) );
        int fuelItemLimit = (int) Math.ceil( fuelSpaceLeft / (double) fuelPerItem );
        if( limit > fuelItemLimit ) limit = fuelItemLimit;

        ItemStack stack = turtle.getItemHandler().extractItem( slot, limit, false );
        int fuelToGive = fuelPerItem * stack.getCount();
        // Store the replacement item in the inventory
        ItemStack replacementStack = stack.getItem().getContainerItem( stack );
        if( !replacementStack.isEmpty() )
        {
            ItemStack remainder = InventoryUtil.storeItems( replacementStack, turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getLevel(), turtle.getPosition(), turtle.getDirection().getOpposite() );
            }
        }

        return fuelToGive;
    }

    private static int getFuelPerItem( @Nonnull ItemStack stack )
    {
        return (ForgeHooks.getBurnTime( stack, null ) * 5) / 100;
    }

    @SubscribeEvent
    public static void onTurtleRefuel( TurtleRefuelEvent event )
    {
        if( event.getHandler() == null && getFuelPerItem( event.getStack() ) > 0 ) event.setHandler( INSTANCE );
    }
}
