/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class TurtleAxe extends TurtleTool
{
    public TurtleAxe( ResourceLocation id, String adjective, Item item )
    {
        super( id, adjective, item );
    }

    public TurtleAxe( ResourceLocation id, Item item )
    {
        super( id, item );
    }

    public TurtleAxe( ResourceLocation id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, craftItem, toolItem );
    }

    @Override
    protected float getDamageMultiplier()
    {
        return 6.0f;
    }
}
