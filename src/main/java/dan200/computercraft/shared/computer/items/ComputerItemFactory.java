/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.IComputerTile;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ComputerItemFactory
{
    @Nonnull
    public static ItemStack create( IComputerTile computerTile )
    {
        IComputer computer = computerTile.getComputer();
        if( computer != null )
        {
            String label = computer.getLabel();
            int id = (label != null) ? computer.getID() : -1;
            return create( id, label, computerTile.getFamily() );
        }
        else
        {
            return create( -1, null, computerTile.getFamily() );
        }
    }

    @Nonnull
    public static ItemStack create( int id, String label, ComputerFamily family )
    {
        switch( family )
        {
            case Normal:
                return ((ItemComputer) Item.getItemFromBlock( ComputerCraft.Blocks.computerNormal )).create( id, label );
            case Advanced:
                return ((ItemComputer) Item.getItemFromBlock( ComputerCraft.Blocks.computerAdvanced )).create( id, label );
            case Command:
                return ((ItemComputer) Item.getItemFromBlock( ComputerCraft.Blocks.computerCommand )).create( id, label );
            default:
                return ItemStack.EMPTY;
        }
    }
}
