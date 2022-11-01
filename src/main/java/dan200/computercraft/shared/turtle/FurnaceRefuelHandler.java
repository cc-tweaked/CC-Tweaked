/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nonnull;
import java.util.OptionalInt;

public final class FurnaceRefuelHandler implements TurtleRefuelHandler
{
    private static final FurnaceRefuelHandler INSTANCE = new FurnaceRefuelHandler();

    private FurnaceRefuelHandler()
    {
    }

    @Override
    public OptionalInt refuel( @Nonnull ITurtleAccess turtle, @Nonnull ItemStack currentStack, int slot, int limit )
    {
        int fuelPerItem = getFuelPerItem( currentStack );
        if( fuelPerItem <= 0 ) return OptionalInt.empty();
        if( limit == 0 ) return OptionalInt.of( 0 );

        int fuelSpaceLeft = turtle.getFuelLimit() - turtle.getFuelLevel();
        int fuelItemLimit = (int) Math.ceil( fuelSpaceLeft / (double) fuelPerItem );
        if( limit > fuelItemLimit ) limit = fuelItemLimit;

        ItemStack stack = turtle.getInventory().removeItem( slot, limit );
        int fuelToGive = fuelPerItem * stack.getCount();
        // Store the replacement item in the inventory
        ItemStack replacementStack = ForgeHooks.getCraftingRemainingItem( stack );
        if( !replacementStack.isEmpty() ) TurtleUtil.storeItemOrDrop( turtle, replacementStack );

        turtle.getInventory().setChanged();

        return OptionalInt.of( fuelToGive );
    }

    private static int getFuelPerItem( @Nonnull ItemStack stack )
    {
        return (ForgeHooks.getBurnTime( stack, null ) * 5) / 100;
    }
}
