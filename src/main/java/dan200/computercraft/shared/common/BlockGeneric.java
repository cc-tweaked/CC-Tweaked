/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public abstract class BlockGeneric extends Block
{
    private final TileEntityType<? extends TileGeneric> type;

    public BlockGeneric( Properties settings, NamedTileEntityType<? extends TileGeneric> type )
    {
        super( settings );
        this.type = type;
        type.setBlock( this );
    }

    @Override
    @Deprecated
    public final void onReplaced( @Nonnull BlockState block, @Nonnull World world, @Nonnull BlockPos pos, BlockState replace, boolean bool )
    {
        if( block.getBlock() == replace.getBlock() ) return;

        TileEntity tile = world.getTileEntity( pos );
        super.onReplaced( block, world, pos, replace, bool );
        world.removeTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).destroy();
    }

    @Override
    @Deprecated
    public final boolean onBlockActivated( BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof TileGeneric && ((TileGeneric) tile).onActivate( player, hand, hit );
    }

    @Override
    @Deprecated
    public final void neighborChanged( BlockState state, World world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourChange( neighbourPos );
    }

    @Override
    public final void onNeighborChange( BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbour )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourTileEntityChange( neighbour );
    }

    @Override
    @Deprecated
    public void tick( BlockState state, World world, BlockPos pos, Random rand )
    {
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TileGeneric ) ((TileGeneric) te).blockTick();
    }

    @Override
    public boolean hasTileEntity( BlockState state )
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull BlockState state, @Nonnull IBlockReader world )
    {
        return type.create();
    }

    @Override
    public boolean canBeReplacedByLeaves( BlockState state, IWorldReader world, BlockPos pos )
    {
        return false;
    }
}
