/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.common.BlockGeneric;
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
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMonitor extends BlockGeneric
{
    public static final DirectionProperty ORIENTATION = DirectionProperty.create( "orientation",
        Direction.UP, Direction.DOWN, Direction.NORTH );

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final EnumProperty<MonitorEdgeState> STATE = EnumProperty.create( "state", MonitorEdgeState.class );

    public BlockMonitor( Properties settings, RegistryObject<? extends TileEntityType<? extends TileMonitor>> type )
    {
        super( settings, type );
        // TODO: Test underwater - do we need isSolid at all?
        setDefaultState( getStateContainer().getBaseState()
            .with( ORIENTATION, Direction.NORTH )
            .with( FACING, Direction.NORTH )
            .with( STATE, MonitorEdgeState.NONE ) );
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
    public void onBlockPlacedBy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState blockState, @Nullable LivingEntity livingEntity, @Nonnull ItemStack itemStack )
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
