/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class CreativeTabTreasure extends CreativeTabs
{
    public CreativeTabTreasure( int i )
    {
        super( i, "Treasure Disks" );
    }
    
    @Nonnull
    @Override
    public ItemStack createIcon()
    {
        return new ItemStack( ComputerCraft.Items.treasureDisk );
    }
    
    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return getTabLabel();
    }
}
