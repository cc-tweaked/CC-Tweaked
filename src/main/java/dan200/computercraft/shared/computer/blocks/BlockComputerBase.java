/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    public static final Identifier COMPUTER_DROP = new Identifier( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;

    protected BlockComputerBase( Settings settings, ComputerFamily family, BlockEntityType<? extends T> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Override
    @Deprecated
    public void onBlockAdded( BlockState state, World world, BlockPos pos, BlockState oldState )
    {
        super.onBlockAdded( state, world, pos, oldState );

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateAllInputs();
    }

    @Override
    @Deprecated
    public boolean emitsRedstonePower( BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getStrongRedstonePower( BlockState state, BlockView world, BlockPos pos, Direction incomingSide )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        Direction localSide = computerEntity.remapToLocalSide( incomingSide.getOpposite() );
        return computerEntity.isRedstoneBlockedOnSide( localSide ) ? 0 :
            computer.getRedstoneOutput( localSide.getId() );
    }

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    @Deprecated
    public int getWeakRedstonePower( BlockState state, BlockView world, BlockPos pos, Direction incomingSide )
    {
        return getStrongRedstonePower( state, world, pos, incomingSide );
    }

    @Override
    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, Direction side )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return false;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        return !computerEntity.isRedstoneBlockedOnSide( computerEntity.remapToLocalSide( side ) );
    }

    @Override
    public int getBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        Direction localSide = computerEntity.remapToLocalSide( side );
        return computerEntity.isRedstoneBlockedOnSide( localSide ) ? 0 :
            computer.getBundledRedstoneOutput( localSide.getId() );
    }

    @Nonnull
    @Override
    public ItemStack getPickStack( BlockView world, BlockPos pos, BlockState state )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() ) return result;
        }

        return super.getPickStack( world, pos, state );
    }

     /*
    @Override
    @Deprecated
    public final void dropBlockAsItemWithChance( @Nonnull BlockState state, World world, @Nonnull BlockPos pos, float change, int fortune )
    {
    }

    @Override
    public void getDrops( BlockState state, DefaultedList<ItemStack> drops, World world, BlockPos pos, int fortune )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack stack = getItem( (TileComputerBase) tile );
            if( !stack.isEmpty() ) drops.add( stack );
        }
    }

    @Override
    public boolean removedByPlayer( BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid )
    {
        if( !world.isClient )
        {
            // We drop the item here instead of doing it in the harvest method, as we
            // need to drop it for creative players too.
            BlockEntity tile = world.getBlockEntity( pos );
            if( tile instanceof TileComputerBase )
            {
                TileComputerBase computer = (TileComputerBase) tile;
                if( !player.abilities.creativeMode || computer.getLabel() != null )
                {
                    dropStack( world, pos, getItem( computer ) );
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest, fluid );
    }
    */
}
