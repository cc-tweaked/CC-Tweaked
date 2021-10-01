/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockPrinter extends BlockGeneric
{
    static final BooleanProperty TOP = BooleanProperty.of( "top" );
    static final BooleanProperty BOTTOM = BooleanProperty.of( "bottom" );
    private static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BlockPrinter( Settings settings )
    {
        super( settings, ComputerCraftRegistry.ModTiles.PRINTER );
        setDefaultState( getStateManager().getDefaultState()
            .with( FACING, Direction.NORTH )
            .with( TOP, false )
            .with( BOTTOM, false ) );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState().with( FACING,
            placement.getPlayerFacing()
                .getOpposite() );
    }

    @Override
    public void afterBreak( @Nonnull World world, @Nonnull PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity te, @Nonnull ItemStack stack )
    {
        if( te instanceof Nameable && ((Nameable) te).hasCustomName() )
        {
            player.incrementStat( Stats.MINED.getOrCreateStat( this ) );
            player.addExhaustion( 0.005F );

            ItemStack result = new ItemStack( this );
            result.setCustomName( ((Nameable) te).getCustomName() );
            dropStack( world, pos, result );
        }
        else
        {
            super.afterBreak( world, player, pos, state, te, stack );
        }
    }

    @Override
    public void onPlaced( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, ItemStack stack )
    {
        if( stack.hasCustomName() )
        {
            BlockEntity tileentity = world.getBlockEntity( pos );
            if( tileentity instanceof TilePrinter )
            {
                ((TilePrinter) tileentity).customName = stack.getName();
            }
        }
    }

    @Override
    protected void appendProperties( StateManager.Builder<Block, BlockState> properties )
    {
        properties.add( FACING, TOP, BOTTOM );
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return new TilePrinter(ComputerCraftRegistry.ModTiles.PRINTER, pos, state);
    }
}
