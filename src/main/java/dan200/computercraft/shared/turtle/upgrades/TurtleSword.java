/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TurtleSword extends TurtleTool
{
    public TurtleSword( ResourceLocation id, String adjective, Item item )
    {
        super( id, adjective, item );
    }

    public TurtleSword( ResourceLocation id, Item item )
    {
        super( id, item );
    }

    public TurtleSword( ResourceLocation id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, craftItem, toolItem );
    }

    @Override
    protected boolean canBreakBlock( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        if( !super.canBreakBlock( state, world, pos, player ) ) return false;

        Material material = state.getMaterial();
        return material == Material.PLANTS ||
            material == Material.LEAVES ||
            material == Material.TALL_PLANTS ||
            material == Material.WOOL ||
            material == Material.WEB;
    }

    @Override
    protected float getDamageMultiplier()
    {
        return 9.0f;
    }
}
