/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMonitor extends BlockGeneric
{
    public static final DirectionProperty ORIENTATION = DirectionProperty.create( "orientation",
        EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH );

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    static final EnumProperty<MonitorEdgeState> STATE = EnumProperty.create( "state", MonitorEdgeState.class );

    public BlockMonitor( Properties settings, TileEntityType<? extends TileGeneric> type )
    {
        super( settings, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( ORIENTATION, EnumFacing.NORTH )
            .with( FACING, EnumFacing.NORTH )
            .with( STATE, MonitorEdgeState.NONE ) );
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, IBlockState> builder )
    {
        builder.add( ORIENTATION, FACING, STATE );
    }

    @Override
    @Nullable
    public IBlockState getStateForPlacement( BlockItemUseContext context )
    {
        float pitch = context.getPlayer() == null ? 0 : context.getPlayer().rotationPitch;
        EnumFacing orientation;
        if( pitch > 66.5f )
        {
            // If the player is looking down, place it facing upwards
            orientation = EnumFacing.UP;
        }
        else if( pitch < -66.5f )
        {
            // If they're looking up, place it down.
            orientation = EnumFacing.DOWN;
        }
        else
        {
            orientation = EnumFacing.NORTH;
        }

        return getDefaultState()
            .with( FACING, context.getPlacementHorizontalFacing().getOpposite() )
            .with( ORIENTATION, orientation );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState blockState, @Nullable EntityLivingBase livingEntity, ItemStack itemStack )
    {
        super.onBlockPlacedBy( world, pos, blockState, livingEntity, itemStack );

        TileEntity entity = world.getTileEntity( pos );
        if( entity instanceof TileMonitor && !world.isRemote )
        {
            TileMonitor monitor = (TileMonitor) entity;
            monitor.contractNeighbours();
            monitor.contract();
            monitor.expand();
        }
    }
}
