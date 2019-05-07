/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.util.NamedBlockEntityType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public abstract class BlockGeneric extends Block implements BlockEntityProvider
{
    private final BlockEntityType<? extends TileGeneric> type;

    public BlockGeneric( Settings settings, NamedBlockEntityType<? extends TileGeneric> type )
    {
        super( settings );
        this.type = type;
        type.setBlock( this );
    }

    @Override
    @Deprecated
    public final void onBlockRemoved( @Nonnull BlockState block, @Nonnull World world, @Nonnull BlockPos pos, BlockState replace, boolean bool )
    {
        if( block.getBlock() == replace.getBlock() ) return;

        BlockEntity tile = world.getBlockEntity( pos );
        super.onBlockRemoved( block, world, pos, replace, bool );
        world.removeBlockEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).destroy();
    }

    @Override
    @Deprecated
    public final boolean activate( BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        return tile instanceof TileGeneric && ((TileGeneric) tile).onActivate( player, hand, hit );
    }

    @Override
    @Deprecated
    public final void neighborUpdate( BlockState state, World world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean flag )
    {
        super.neighborUpdate( state, world, pos, neighbourBlock, neighbourPos, flag );
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileGeneric ) ((TileGeneric) tile).onNeighbourChange( neighbourPos );
    }

    @Override
    @Deprecated
    public void onScheduledTick( BlockState state, World world, BlockPos pos, Random rand )
    {
        BlockEntity te = world.getBlockEntity( pos );
        if( te instanceof TileGeneric ) ((TileGeneric) te).blockTick();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity( BlockView blockView )
    {
        return type.instantiate();
    }
}
