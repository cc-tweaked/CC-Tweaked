/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public final class ComputerItemFactory
{
    private ComputerItemFactory() {}

    @Nonnull
    public static ItemStack create( TileComputer tile )
    {
        return create( tile.getComputerID(), tile.getLabel(), tile.getFamily() );
    }

    @Nonnull
    public static ItemStack create( int id, String label, ComputerFamily family )
    {
        switch( family )
        {
            case NORMAL:
                return Registry.ModItems.COMPUTER_NORMAL.get().create( id, label );
            case ADVANCED:
                return Registry.ModItems.COMPUTER_ADVANCED.get().create( id, label );
            case COMMAND:
                return Registry.ModItems.COMPUTER_COMMAND.get().create( id, label );
            default:
                return ItemStack.EMPTY;
        }
    }
}
