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
import dan200.computercraft.shared.ComputerCraftTags;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class TurtleShovel extends TurtleTool
{
    public TurtleShovel( ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem )
    {
        super( id, adjective, craftItem, toolItem );
    }

    @Override
    protected TurtleCommandResult checkBlockBreakable( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        TurtleCommandResult result = super.checkBlockBreakable( state, world, pos, player );
        if( !result.isSuccess() ) return result;

        return state.is( ComputerCraftTags.Blocks.TURTLE_SHOVEL_BREAKABLE )
            || isTriviallyBreakable( world, pos, state )
            ? result : INEFFECTIVE;
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        if( verb == TurtleVerb.DIG )
        {
            if( TurtlePlaceCommand.deployCopiedItem( item.copy(), turtle, direction, null, null ) )
            {
                return TurtleCommandResult.success();
            }
        }
        return super.useTool( turtle, side, verb, direction );
    }
}
