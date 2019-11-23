/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMonitor extends BlockGeneric
{
    public static final DirectionProperty ORIENTATION = DirectionProperty.create( "orientation",
        Direction.UP, Direction.DOWN, Direction.NORTH );

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    static final EnumProperty<MonitorEdgeState> STATE = EnumProperty.create( "state", MonitorEdgeState.class );

    public BlockMonitor( Properties settings, NamedTileEntityType<? extends TileGeneric> type )
    {
        super( settings, type );
        setDefaultState( getStateContainer().getBaseState()
            .with( ORIENTATION, Direction.NORTH )
            .with( FACING, Direction.NORTH )
            .with( STATE, MonitorEdgeState.NONE ) );
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer()
    {
        // We use the CUTOUT layer, as otherwise monitor rendering will cause flickering.
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @Deprecated
    public boolean isSolid( BlockState state )
    {
        // We override isSolid, as our overriding of getRenderLayer means that it would otherwise return false.
        return true;
    }

    @Override
    protected void fillStateContainer( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( ORIENTATION, FACING, STATE );
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement( BlockItemUseContext context )
    {
        float pitch = context.getPlayer() == null ? 0 : context.getPlayer().rotationPitch;
        Direction orientation;
        if( pitch > 66.5f )
        {
            // If the player is looking down, place it facing upwards
            orientation = Direction.UP;
        }
        else if( pitch < -66.5f )
        {
            // If they're looking up, place it down.
            orientation = Direction.DOWN;
        }
        else
        {
            orientation = Direction.NORTH;
        }

        return getDefaultState()
            .with( FACING, context.getPlacementHorizontalFacing().getOpposite() )
            .with( ORIENTATION, orientation );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack )
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
