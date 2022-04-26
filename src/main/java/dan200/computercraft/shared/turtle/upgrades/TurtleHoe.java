/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;

public class TurtleHoe extends TurtleTool
{
    public TurtleHoe( ResourceLocation id, String adjective, Item item )
    {
        super( id, adjective, item );
    }

    public TurtleHoe( ResourceLocation id, Item item )
    {
        super( id, item );
    }

    public TurtleHoe( ResourceLocation id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, craftItem, toolItem );
    }

    @Override
    protected TurtleCommandResult checkBlockBreakable( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        TurtleCommandResult result = super.checkBlockBreakable( state, world, pos, player );
        if( !result.isSuccess() ) return result;

        return state.isToolEffective( ToolType.HOE )
            || state.is( ComputerCraftTags.Blocks.TURTLE_HOE_BREAKABLE )
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
