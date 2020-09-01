/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.diskdrive;

import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockDiskDrive extends BlockGeneric
{
    static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    static final EnumProperty<DiskDriveState> STATE = EnumProperty.of( "state", DiskDriveState.class );

    public BlockDiskDrive( Settings settings )
    {
        super( settings, ComputerCraftRegistry.ModTiles.DISK_DRIVE );
        setDefaultState( getStateManager().getDefaultState()
            .with( FACING, Direction.NORTH )
            .with( STATE, DiskDriveState.EMPTY ) );
    }


    @Override
    protected void appendProperties( StateManager.Builder<Block, BlockState> properties )
    {
        properties.add( FACING, STATE );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState().with( FACING, placement.getPlayerFacing().getOpposite() );
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
            if( tileentity instanceof TileDiskDrive ) ((TileDiskDrive) tileentity).customName = stack.getName();
        }
    }
}
