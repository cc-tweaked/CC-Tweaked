/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockComputerBase extends BlockGeneric implements IBundledRedstoneBlock
{
    public BlockComputerBase( Material material )
    {
        super( material );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        return "tile." + getRegistryName();
    }

    @Override
    public void onBlockAdded( World world, BlockPos pos, IBlockState state )
    {
        super.onBlockAdded( world, pos, state );
        updateInput( world, pos );
    }

    protected abstract IBlockState getDefaultBlockState( EnumFacing placedSide );

    protected abstract ComputerFamily getFamily();

    @Override
    @Deprecated
    public final boolean canProvidePower( IBlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public final int getStrongPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase && tile.hasWorld() )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            return computer.getRedstoneOutput( oppositeSide.getOpposite() );
        }
        return 0;
    }

    @Override
    @Deprecated
    public final int getWeakPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        return getStrongPower( state, world, pos, oppositeSide );
    }

    @Override
    public int getBundledRedstoneOutput( @Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && tile.hasWorld() ? ((TileComputerBase) tile).getBundledRedstoneOutput( side ) : 0;
    }


    @Override
    public final void dropBlockAsItemWithChance( World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, float chance, int fortune )
    {
    }

    @Override
    public final void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        getDroppedItems( state, world, pos, drops, false );
    }

    @Override
    public boolean removedByPlayer( @Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest )
    {
        if( !world.isRemote )
        {
            NonNullList<ItemStack> drops = NonNullList.create();
            getDroppedItems( state, world, pos, drops, player.capabilities.isCreativeMode );
            for( ItemStack item : drops ) Block.spawnAsEntity( world, pos, item );
        }
        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }

    protected void getDroppedItems( IBlockState state, IBlockAccess world, BlockPos pos, @Nonnull NonNullList<ItemStack> drops, boolean creative )
    {
        if( !creative )
        {
            ItemStack drop = getComputerItem( state, world, pos );
            if( !drop.isEmpty() ) drops.add( drop );
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public ItemStack getItem( World world, BlockPos pos, @Nonnull IBlockState state )
    {
        return getComputerItem( state, world, pos );
    }

    protected abstract ItemStack getComputerItem( IBlockState state, IBlockAccess world, BlockPos pos );

    protected void updateInput( IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }
}
