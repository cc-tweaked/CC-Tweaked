/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockPrinter extends BlockGeneric
{
    static final BooleanProperty TOP = BooleanProperty.create( "top" );
    static final BooleanProperty BOTTOM = BooleanProperty.create( "bottom" );
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockPrinter( Properties settings )
    {
        super( settings, () -> ComputerCraftRegistry.ModTiles.PRINTER );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( TOP, false )
            .setValue( BOTTOM, false ) );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState().setValue( FACING,
            placement.getHorizontalDirection()
                .getOpposite() );
    }

    @Override
    public void playerDestroy( @Nonnull Level world, @Nonnull Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity te, @Nonnull ItemStack stack )
    {
        if( te instanceof Nameable && ((Nameable) te).hasCustomName() )
        {
            player.awardStat( Stats.BLOCK_MINED.get( this ) );
            player.causeFoodExhaustion( 0.005F );

            ItemStack result = new ItemStack( this );
            result.setHoverName( ((Nameable) te).getCustomName() );
            popResource( world, pos, result );
        }
        else
        {
            super.playerDestroy( world, player, pos, state, te, stack );
        }
    }

    @Override
    public void setPlacedBy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, ItemStack stack )
    {
        if( stack.hasCustomHoverName() )
        {
            BlockEntity tileentity = world.getBlockEntity( pos );
            if( tileentity instanceof TilePrinter )
            {
                ((TilePrinter) tileentity).customName = stack.getHoverName();
            }
        }
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> properties )
    {
        properties.add( FACING, TOP, BOTTOM );
    }
}
