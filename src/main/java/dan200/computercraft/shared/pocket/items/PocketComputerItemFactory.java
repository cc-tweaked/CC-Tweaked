/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public final class PocketComputerItemFactory
{
    private PocketComputerItemFactory() {}

    @Nonnull
    public static ItemStack create( int id, String label, int colour, ComputerFamily family, IPocketUpgrade upgrade )
    {
        switch( family )
        {
            case Normal:
            case Advanced:
                return ComputerCraft.Items.pocketComputer.create( id, label, colour, family, upgrade );
            default:
                return ItemStack.EMPTY;
        }
    }
}
