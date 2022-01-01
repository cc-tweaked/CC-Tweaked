/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class CreativeTabMain extends CreativeModeTab
{
    public CreativeTabMain()
    {
        super( ComputerCraft.MOD_ID );
    }

    @Nonnull
    @Override
    public ItemStack makeIcon()
    {
        return new ItemStack( Registry.ModBlocks.COMPUTER_NORMAL.get() );
    }
}
