/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;

public class ItemTurtleAdvanced extends ItemTurtleNormal
{
    public ItemTurtleAdvanced( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:advanced_turtle" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    // IComputerItem implementation

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return ComputerFamily.Advanced;
    }
}
