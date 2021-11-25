/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TurtleAxe extends TurtleTool
{
    public TurtleAxe( ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem )
    {
        super( id, adjective, craftItem, toolItem );
    }

    @Override
    protected float getDamageMultiplier()
    {
        return 6.0f;
    }
}
