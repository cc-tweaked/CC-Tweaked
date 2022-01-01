/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.upgrades.TurtleInventoryCrafting;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtleCraftCommand implements ITurtleCommand
{
    private final int limit;

    public TurtleCraftCommand( int limit )
    {
        this.limit = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Craft the item
        TurtleInventoryCrafting crafting = new TurtleInventoryCrafting( turtle );
        List<ItemStack> results = crafting.doCrafting( turtle.getLevel(), limit );
        if( results == null ) return TurtleCommandResult.failure( "No matching recipes" );

        // Store or drop any remainders
        for( ItemStack stack : results )
        {
            ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getLevel(), turtle.getPosition(), turtle.getDirection() );
            }
        }

        if( !results.isEmpty() ) turtle.playAnimation( TurtleAnimation.WAIT );
        return TurtleCommandResult.success();
    }
}
