/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public final class ComputerItemFactory
{
    private ComputerItemFactory() {}

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
                return ComputerCraft.Items.computerNormal.create( id, label );
            case Advanced:
                return ComputerCraft.Items.computerAdvanced.create( id, label );
            case Command:
                return ComputerCraft.Items.computerCommand.create( id, label );
            default:
                return ItemStack.EMPTY;
        }
    }
}
