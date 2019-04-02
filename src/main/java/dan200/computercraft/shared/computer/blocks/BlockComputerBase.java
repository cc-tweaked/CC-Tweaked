/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.common.BlockDirectional;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockComputerBase extends BlockDirectional
{
    public BlockComputerBase( Material material )
    {
        super( material );
    }

    @Override
    public void onBlockAdded( World world, BlockPos pos, IBlockState state )
    {
        super.onBlockAdded( world, pos, state );
        updateInput( world, pos );
    }

    @Override
    public void setDirection( World world, BlockPos pos, EnumFacing dir )
    {
        super.setDirection( world, pos, dir );
        updateInput( world, pos );
    }

    protected abstract IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide );

    protected abstract ComputerFamily getFamily( int damage );

    protected abstract ComputerFamily getFamily( IBlockState state );

    protected abstract TileComputerBase createTile( ComputerFamily family );

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    @Nonnull
    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        return getDefaultBlockState( getFamily( damage ), DirectionUtil.fromEntityRot( placer ) );
    }

    @Override
    public final TileComputerBase createTile( IBlockState state )
    {
        return createTile( getFamily( state ) );
    }

    @Override
    public final TileComputerBase createTile( int damage )
    {
        return createTile( getFamily( damage ) );
    }

    public final ComputerFamily getFamily( IBlockAccess world, BlockPos pos )
    {
        return getFamily( world.getBlockState( pos ) );
    }

    private static void updateInput( IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() ) return result;
        }

        return super.getPickBlock( state, target, world, pos, player );
    }

    @Override
    public final void dropBlockAsItemWithChance( World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, float chance, int fortune )
    {
    }

    @Override
    public final void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack stack = getItem( (TileComputerBase) tile );
            if( !stack.isEmpty() ) drops.add( stack );
        }
    }

    @Override
    public boolean removedByPlayer( @Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest )
    {
        if( !world.isRemote )
        {
            // We drop the item here instead of doing it in the harvest method, as we
            // need to drop it for creative players too.
            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof TileComputerBase )
            {
                TileComputerBase computer = (TileComputerBase) tile;
                if( !player.capabilities.isCreativeMode || computer.getLabel() != null )
                {
                    spawnAsEntity( world, pos, getItem( computer ) );
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
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
