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

public class TurtleSword extends TurtleTool
{
    public TurtleSword( ResourceLocation id, Item item, float damageMulitiplier )
    {
        super( id, item, damageMulitiplier );
    }

    @Override
    protected TurtleCommandResult checkBlockBreakable( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        TurtleCommandResult defaultResult = super.checkBlockBreakable( state, world, pos, player );
        if( defaultResult.isSuccess() ) return defaultResult;

        Material material = state.getMaterial();
        return material == Material.PLANT || material == Material.LEAVES || material == Material.REPLACEABLE_PLANT || material == Material.WOOL || material == Material.WEB
            ? TurtleCommandResult.success()
            : INEFFECTIVE;
    }
}
