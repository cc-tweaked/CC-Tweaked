/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.Registry;
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
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final BooleanProperty TOP = BooleanProperty.create( "top" );
    static final BooleanProperty BOTTOM = BooleanProperty.create( "bottom" );

    public BlockPrinter( Properties settings )
    {
        super( settings, Registry.ModBlockEntities.PRINTER );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( TOP, false )
            .setValue( BOTTOM, false ) );
    }

    @Override
    protected void createBlockStateDefinition( StateDefinition.Builder<Block, BlockState> properties )
    {
        properties.add( FACING, TOP, BOTTOM );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockPlaceContext placement )
    {
        return defaultBlockState().setValue( FACING, placement.getHorizontalDirection().getOpposite() );
    }

    @Override
    public void playerDestroy( @Nonnull Level world, @Nonnull Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity te, @Nonnull ItemStack stack )
    {
        if( te instanceof Nameable nameable && nameable.hasCustomName() )
        {
            player.awardStat( Stats.BLOCK_MINED.get( this ) );
            player.causeFoodExhaustion( 0.005F );

            ItemStack result = new ItemStack( this );
            result.setHoverName( nameable.getCustomName() );
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
        if( stack.hasCustomHoverName() && world.getBlockEntity( pos ) instanceof TilePrinter printer )
        {
            printer.customName = stack.getHoverName();
        }
    }
}
