/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class TurtleCraftCommand implements ITurtleCommand
{
    private final int m_limit;

    public TurtleCraftCommand( int limit )
    {
        m_limit = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Craft the item
        TurtleInventoryCrafting crafting = new TurtleInventoryCrafting( turtle );
        ArrayList<ItemStack> results = crafting.doCrafting( turtle.getWorld(), m_limit );
        if( results != null )
        {
            // Store the results
            for( ItemStack stack : results )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    // Drop the remainder
                    BlockPos position = turtle.getPosition();
                    WorldUtil.dropItemStack( remainder, turtle.getWorld(), position, turtle.getDirection() );
                }
            }

            if( results.size() > 0 )
            {
                // Animate
                turtle.playAnimation( TurtleAnimation.Wait );
            }

            // Succeed
            return TurtleCommandResult.success();
        }

        // Fail
        return TurtleCommandResult.failure( "No matching recipes" );
    }
}
