/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

public abstract class BlockGeneric extends Block implements ITileEntityProvider
{
    protected BlockGeneric( Material material )
    {
        super( material );
        this.hasTileEntity = true;
    }

    protected abstract TileGeneric createTile( IBlockState state );

    protected abstract TileGeneric createTile( int damage );

    @Override
    public final void breakBlock( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState newState )
    {
        TileEntity tile = world.getTileEntity( pos );
        super.breakBlock( world, pos, newState );
        world.removeTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).destroy();
    }

    @Override
    public final boolean onBlockActivated( World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && ((TileGeneric) tile).onActivate( player, hand, side, hitX, hitY, hitZ );
    }

    @Override
    @Deprecated
    @SuppressWarnings( "deprecation" )
    public final void neighborChanged( IBlockState state, World world, BlockPos pos, Block block, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourChange( neighbour );
    }

    @Override
    public final void onNeighborChange( IBlockAccess world, BlockPos pos, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void updateTick( World world, BlockPos pos, IBlockState state, Random rand )
    {
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TileGeneric ) ((TileGeneric) te).updateTick();
    }

    @Override
    @Deprecated
    public final boolean canProvidePower( IBlockState state )
    {
        return true;
    }

    @Override
    public final boolean canConnectRedstone( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && ((TileGeneric) tile).getRedstoneConnectivity( side );
    }

    @Override
    @Deprecated
    public final int getStrongPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric && tile.hasWorld() )
        {
            return ((TileGeneric) tile).getRedstoneOutput( oppositeSide.getOpposite() );
        }
        return 0;
    }

    @Override
    @Deprecated
    public final int getWeakPower( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing oppositeSide )
    {
        return getStrongPower( state, world, pos, oppositeSide );
    }

    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric )
        {
            return ((TileGeneric) tile).getBundledRedstoneConnectivity( side );
        }
        return false;
    }

    public int getBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric && tile.hasWorld() )
        {
            return ((TileGeneric) tile).getBundledRedstoneOutput( side );
        }
        return 0;
    }

    @Nonnull
    @Override
    public final TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return createTile( state );
    }

    @Nonnull
    @Override
    public final TileEntity createNewTileEntity( @Nonnull World world, int damage )
    {
        return createTile( damage );
    }

    @Override
    @Deprecated
    public boolean isSideSolid( IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side )
    {
        // We need to override this as the default implementation uses isNormalCube, which returns false if
        // it can provide power.
        return isFullCube( state );
    }
}
