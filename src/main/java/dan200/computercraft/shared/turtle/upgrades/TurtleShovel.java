/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleVerb;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class TurtleShovel extends TurtleTool
{
    public TurtleShovel( ResourceLocation id, String adjective, Item item )
    {
        super( id, adjective, item );
    }

    public TurtleShovel( ResourceLocation id, Item item )
    {
        super( id, item );
    }

    public TurtleShovel( ResourceLocation id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, craftItem, toolItem );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        if( verb == TurtleVerb.DIG )
        {
            ItemStack shovel = item.copy();
            ItemStack remainder = TurtlePlaceCommand.deploy( shovel, turtle, direction, null, null );
            if( remainder != shovel )
            {
                return TurtleCommandResult.success();
            }
        }
        return super.useTool( turtle, side, verb, direction );
    }

    @Override
    protected boolean canBreakBlock( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        if( !super.canBreakBlock( state, world, pos, player ) )
        {
            return false;
        }

        Material material = state.getMaterial();
        return material == Material.DIRT || material == Material.SAND || material == Material.TOP_SNOW || material == Material.CLAY || material == Material.SNOW || material == Material.PLANT || material == Material.CACTUS || material == Material.VEGETABLE || material == Material.LEAVES || material == Material.REPLACEABLE_PLANT;
    }
}
