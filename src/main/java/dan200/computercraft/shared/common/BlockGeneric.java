/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public abstract class BlockGeneric extends BaseEntityBlock
{
    private final RegistryObject<? extends BlockEntityType<? extends TileGeneric>> type;

    public BlockGeneric( Properties settings, RegistryObject<? extends BlockEntityType<? extends TileGeneric>> type )
    {
        super( settings );
        this.type = type;
    }

    @Override
    @Deprecated
    public final void onRemove( @Nonnull BlockState block, @Nonnull Level world, @Nonnull BlockPos pos, BlockState replace, boolean bool )
    {
        if( block.getBlock() == replace.getBlock() ) return;

        BlockEntity tile = world.getBlockEntity( pos );
        super.onRemove( block, world, pos, replace, bool );
        world.removeBlockEntity( pos );
        if( tile instanceof TileGeneric generic ) generic.destroy();
    }

    @Nonnull
    @Override
    @Deprecated
    public final InteractionResult use( @Nonnull BlockState state, Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        return tile instanceof TileGeneric generic ? generic.onActivate( player, hand, hit ) : InteractionResult.PASS;
    }

    @Override
    @Deprecated
    public final void neighborChanged( @Nonnull BlockState state, Level world, @Nonnull BlockPos pos, @Nonnull Block neighbourBlock, @Nonnull BlockPos neighbourPos, boolean isMoving )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileGeneric generic ) generic.onNeighbourChange( neighbourPos );
    }

    @Override
    public final void onNeighborChange( BlockState state, LevelReader world, BlockPos pos, BlockPos neighbour )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileGeneric generic ) generic.onNeighbourTileEntityChange( neighbour );
    }

    @Override
    @Deprecated
    public void tick( @Nonnull BlockState state, ServerLevel world, @Nonnull BlockPos pos, @Nonnull Random rand )
    {
        BlockEntity te = world.getBlockEntity( pos );
        if( te instanceof TileGeneric generic ) generic.blockTick();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity( @Nonnull BlockPos pos, @Nonnull BlockState state )
    {
        return type.get().create( pos, state );
    }

    @Nonnull
    @Override
    @Deprecated
    public RenderShape getRenderShape( @Nonnull BlockState state )
    {
        return RenderShape.MODEL;
    }
}
