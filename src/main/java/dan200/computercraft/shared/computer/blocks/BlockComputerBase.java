/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private final ComputerFamily family;

    protected BlockComputerBase( Properties settings, ComputerFamily family, NamedTileEntityType<? extends T> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Override
    @Deprecated
    public void onBlockAdded( BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving )
    {
        super.onBlockAdded( state, world, pos, oldState, isMoving );

        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }

    @Override
    @Deprecated
    public boolean canProvidePower( BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getStrongPower( BlockState state, IBlockReader world, BlockPos pos, Direction incomingSide )
    {
        TileEntity entity = world.getTileEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        ComputerSide localSide = computerEntity.remapToLocalSide( incomingSide.getOpposite() );
        return computer.getRedstoneOutput( localSide );
    }

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    @Deprecated
    public int getWeakPower( BlockState state, IBlockReader world, BlockPos pos, Direction incomingSide )
    {
        return getStrongPower( state, world, pos, incomingSide );
    }

    @Override
    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, Direction side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        TileEntity entity = world.getTileEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        ComputerSide localSide = computerEntity.remapToLocalSide( side );
        return computer.getBundledRedstoneOutput( localSide );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() ) return result;
        }

        return super.getPickBlock( state, target, world, pos, player );
    }

    /* TODO: THIS!!
    @Override
    @Deprecated
    public final void dropBlockAsItemWithChance( @Nonnull BlockState state, World world, @Nonnull BlockPos pos, float change, int fortune )
    {
    }

    @Override
    public final void getDrops( BlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack stack = getItem( (TileComputerBase) tile );
            if( !stack.isEmpty() ) drops.add( stack );
        }
    }
    */

    @Override
    public boolean removedByPlayer( BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid )
    {
        if( !world.isRemote )
        {
            // We drop the item here instead of doing it in the harvest method, as we
            // need to drop it for creative players too.
            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof TileComputerBase )
            {
                TileComputerBase computer = (TileComputerBase) tile;
                if( !player.abilities.isCreativeMode || computer.getLabel() != null )
                {
                    spawnAsEntity( world, pos, getItem( computer ) );
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest, fluid );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack )
    {
        super.onBlockPlacedBy( world, pos, state, placer, stack );

        TileEntity tile = world.getTileEntity( pos );
        if( !world.isRemote && tile instanceof IComputerTile && stack.getItem() instanceof IComputerItem )
        {
            IComputerTile computer = (IComputerTile) tile;
            IComputerItem item = (IComputerItem) stack.getItem();

            int id = item.getComputerID( stack );
            if( id != -1 ) computer.setComputerID( id );

            String label = item.getLabel( stack );
            if( label != null ) computer.setLabel( label );
        }
    }
}
