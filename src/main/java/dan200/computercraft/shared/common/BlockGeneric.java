/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public abstract class BlockGeneric extends Block implements ITileEntityProvider
{
    private final TileEntityType<? extends TileGeneric> type;

    public BlockGeneric( Properties settings, TileEntityType<? extends TileGeneric> type )
    {
        super( settings );
        this.type = type;
    }

    @Override
    @Deprecated
    public final void onReplaced( @Nonnull IBlockState block, @Nonnull World world, @Nonnull BlockPos pos, IBlockState replace, boolean bool )
    {
        if( block.getBlock() == replace.getBlock() ) return;

        TileEntity tile = world.getTileEntity( pos );
        super.onReplaced( block, world, pos, replace, bool );
        world.removeTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).destroy();
    }

    @Override
    @Deprecated
    public final boolean onBlockActivated( IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && ((TileGeneric) tile).onActivate( player, hand, side, hitX, hitY, hitZ );
    }

    @Override
    @Deprecated
    @SuppressWarnings( "deprecation" )
    public final void neighborChanged( IBlockState state, World world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourChange( neighbourPos );
    }

    @Override
    public final void onNeighborChange( IBlockState state, IWorldReader world, BlockPos pos, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourTileEntityChange( neighbour );
    }

    @Override
    @Deprecated
    public void tick( IBlockState state, World world, BlockPos pos, Random rand )
    {
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TileGeneric ) ((TileGeneric) te).blockTick();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity( @Nonnull IBlockReader world )
    {
        return type.create();
    }
}
