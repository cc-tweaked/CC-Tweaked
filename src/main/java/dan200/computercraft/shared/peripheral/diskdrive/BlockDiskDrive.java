/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.INameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.AbstractBlock.Properties;

public class BlockDiskDrive extends BlockGeneric
{
    static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final EnumProperty<DiskDriveState> STATE = EnumProperty.create( "state", DiskDriveState.class );

    public BlockDiskDrive( Properties settings )
    {
        super( settings, Registry.ModTiles.DISK_DRIVE );
        registerDefaultState( getStateDefinition().any()
            .setValue( FACING, Direction.NORTH )
            .setValue( STATE, DiskDriveState.EMPTY ) );
    }


    @Override
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> properties )
    {
        properties.add( FACING, STATE );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement( BlockItemUseContext placement )
    {
        return defaultBlockState().setValue( FACING, placement.getHorizontalDirection().getOpposite() );
    }

    @Override
    public void playerDestroy( @Nonnull World world, @Nonnull PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable TileEntity te, @Nonnull ItemStack stack )
    {
        if( te instanceof INameable && ((INameable) te).hasCustomName() )
        {
            player.awardStat( Stats.BLOCK_MINED.get( this ) );
            player.causeFoodExhaustion( 0.005F );

            ItemStack result = new ItemStack( this );
            result.setHoverName( ((INameable) te).getCustomName() );
            popResource( world, pos, result );
        }
        else
        {
            super.playerDestroy( world, player, pos, state, te, stack );
        }
    }

    @Override
    public void setPlacedBy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, ItemStack stack )
    {
        if( stack.hasCustomHoverName() )
        {
            TileEntity tileentity = world.getBlockEntity( pos );
            if( tileentity instanceof TileDiskDrive ) ((TileDiskDrive) tileentity).customName = stack.getHoverName();
        }
    }
}
