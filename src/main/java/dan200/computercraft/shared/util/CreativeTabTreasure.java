/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public abstract class CreativeTabTreasure extends ItemGroup
{
    public CreativeTabTreasure( int i )
    {
        super( i, "Treasure Disks" );
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public ItemStack createIcon()
    {
        return new ItemStack( ComputerCraft.Items.treasureDisk );
    }

}
