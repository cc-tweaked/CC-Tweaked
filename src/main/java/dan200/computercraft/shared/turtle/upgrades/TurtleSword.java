/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.ComputerCraftTags;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.block.BlockState;
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
    protected TurtleCommandResult checkBlockBreakable( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        TurtleCommandResult result = super.checkBlockBreakable( state, world, pos, player );
        if( !result.isSuccess() ) return result;

        return state.is( ComputerCraftTags.Blocks.TURTLE_SWORD_BREAKABLE )
            || isTriviallyBreakable( world, pos, state )
            ? result : INEFFECTIVE;
    }

    @Override
    protected float getDamageMultiplier()
    {
        return 9.0f;
    }
}
