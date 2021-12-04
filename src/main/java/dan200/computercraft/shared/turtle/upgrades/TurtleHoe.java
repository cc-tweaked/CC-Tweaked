/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class TurtleHoe extends TurtleTool
{

    public TurtleHoe( ResourceLocation id, Item item, float damageMulitiplier )
    {
        super( id, item, damageMulitiplier );
    }

    @Override
    protected TurtleCommandResult checkBlockBreakable( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        if( super.checkBlockBreakable( state, world, pos, player ) == TurtleCommandResult.failure() )
        {
            return TurtleCommandResult.failure();
        }

        Material material = state.getMaterial();
        if( material == Material.PLANT || material == Material.CACTUS || material == Material.VEGETABLE
            || material == Material.LEAVES || material == Material.WATER_PLANT || material == Material.REPLACEABLE_PLANT ) return TurtleCommandResult.success();
        return TurtleCommandResult.failure();
    }
}
