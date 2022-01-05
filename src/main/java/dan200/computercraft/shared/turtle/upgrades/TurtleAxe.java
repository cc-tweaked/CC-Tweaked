/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class TurtleAxe extends TurtleTool
{
    public TurtleAxe( Identifier id, String adjective, Item item )
    {
        super( id, adjective, item );
    }

    public TurtleAxe( Identifier id, Item item )
    {
        super( id, item );
    }

    public TurtleAxe( Identifier id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, craftItem, toolItem );
    }

    @Override
    protected float getDamageMultiplier()
    {
        return 6.0f;
    }
}
