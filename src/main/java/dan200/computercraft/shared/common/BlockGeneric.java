/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockGeneric extends Block implements ITileEntityProvider
{
    protected BlockGeneric( Material material )
    {
        super( material );
        this.hasTileEntity = true;
    }

    protected abstract IBlockState getDefaultBlockState( EnumFacing placedSide );

    @Nonnull
    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        return getDefaultBlockState( side );
    }

    @Override
    public final void breakBlock( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState newState )
    {
        TileEntity tile = world.getTileEntity( pos );
        super.breakBlock( world, pos, newState );
        world.removeTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.destroy();
        }
    }

    @Override
    public final boolean onBlockActivated( World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            return generic.onActivate( player, side, hitX, hitY, hitZ );
        }
        return false;
    }

    @Override
    @Deprecated
    public final void neighborChanged( IBlockState state, World world, BlockPos pos, Block block, BlockPos neighorPos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.onNeighbourChange();
        }
    }

    @Override
    public final void onNeighborChange( IBlockAccess world, BlockPos pos, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            TileGeneric generic = (TileGeneric) tile;
            generic.onNeighbourTileEntityChange( neighbour );
        }
    }

    @Nullable
    @Override
    public abstract TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state );

    @Nullable
    @Override
    @SuppressWarnings( "deprecation" )
    public TileEntity createNewTileEntity( @Nonnull World worldIn, int meta )
    {
        return createTileEntity( worldIn, getStateFromMeta( meta ) );
    }
}
