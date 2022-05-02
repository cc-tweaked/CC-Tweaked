/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
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
        registerDefaultState( getStateDefinition().any()
            .setValue( ORIENTATION, Direction.NORTH )
            .setValue( FACING, Direction.NORTH )
            .setValue( STATE, MonitorEdgeState.NONE ) );
    }

    @Override
    protected void createBlockStateDefinition( StateContainer.Builder<Block, BlockState> builder )
    {
        builder.add( ORIENTATION, FACING, STATE );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState mirror( BlockState state, Mirror mirrorIn )
    {
        return state.rotate( mirrorIn.getRotation( state.getValue( FACING ) ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState rotate( BlockState state, Rotation rot )
    {
        return state.setValue( FACING, rot.rotate( state.getValue( FACING ) ) );
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement( BlockItemUseContext context )
    {
        float pitch = context.getPlayer() == null ? 0 : context.getPlayer().xRot;
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

        return defaultBlockState()
            .setValue( FACING, context.getHorizontalDirection().getOpposite() )
            .setValue( ORIENTATION, orientation );
    }

    @Override
    public void setPlacedBy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState blockState, @Nullable LivingEntity livingEntity, @Nonnull ItemStack itemStack )
    {
        super.setPlacedBy( world, pos, blockState, livingEntity, itemStack );

        TileEntity entity = world.getBlockEntity( pos );
        if( entity instanceof TileMonitor && !world.isClientSide )
        {
            TileMonitor monitor = (TileMonitor) entity;
            // Defer the block update if we're being placed by another TE. See #691
            if( livingEntity == null || livingEntity instanceof FakePlayer )
            {
                monitor.updateNeighborsDeferred();
                return;
            }

            monitor.expand();
        }
    }
}
