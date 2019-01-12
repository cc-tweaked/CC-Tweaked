/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ComputerItemFactory
{
    @Nonnull
    public static ItemStack create( TileComputer tile )
    {
        String label = tile.getLabel();
        int id = label != null ? tile.getComputerID() : -1;
        return create( id, label, tile.getFamily() );
    }

    @Nonnull
    public static ItemStack create( int id, String label, ComputerFamily family )
    {
        switch( family )
        {
            case Normal:
            case Advanced:
                return ComputerCraft.Items.computer.create( id, label, family );
            case Command:
                return ComputerCraft.Items.commandComputer.create( id, label, family );
            default:
                return ItemStack.EMPTY;
        }
    }
}
