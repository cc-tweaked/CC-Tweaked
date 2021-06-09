/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private static final Identifier DROP = new Identifier( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;

    protected BlockComputerBase( Settings settings, ComputerFamily family, BlockEntityType<? extends T> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Override
    @Deprecated
    public void onBlockAdded( @Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving )
    {
        super.onBlockAdded( state, world, pos, oldState, isMoving );

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ((TileComputerBase) tile).updateInput();
        }
    }

    @Override
    @Deprecated
    public boolean emitsRedstonePower( @Nonnull BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getWeakRedstonePower( @Nonnull BlockState state, @Nonnull BlockView world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        return this.getStrongRedstonePower( state, world, pos, incomingSide );
    }

    @Override
    @Deprecated
    public int getStrongRedstonePower( @Nonnull BlockState state, BlockView world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase) )
        {
            return 0;
        }

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null )
        {
            return 0;
        }

        ComputerSide localSide = computerEntity.remapToLocalSide( incomingSide.getOpposite() );
        return computer.getRedstoneOutput( localSide );
    }

    public ComputerFamily getFamily()
    {
        return this.family;
    }

    @Override
    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, Direction side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase) )
        {
            return 0;
        }

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null )
        {
            return 0;
        }

        ComputerSide localSide = computerEntity.remapToLocalSide( side );
        return computer.getBundledRedstoneOutput( localSide );
    }

    @Override
    public void afterBreak( @Nonnull World world, PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity tile,
                            @Nonnull ItemStack tool )
    {
        // Don't drop blocks here - see onBlockHarvested.
        player.incrementStat( Stats.MINED.getOrCreateStat( this ) );
        player.addExhaustion( 0.005F );
    }

    @Override
    public void onPlaced( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        super.onPlaced( world, pos, state, placer, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClient && tile instanceof IComputerTile && stack.getItem() instanceof IComputerItem )
        {
            IComputerTile computer = (IComputerTile) tile;
            IComputerItem item = (IComputerItem) stack.getItem();

            int id = item.getComputerID( stack );
            if( id != -1 )
            {
                computer.setComputerID( id );
            }

            String label = item.getLabel( stack );
            if( label != null )
            {
                computer.setLabel( label );
            }
        }
    }

    @Nonnull
    @Override
    public ItemStack getPickStack( BlockView world, BlockPos pos, BlockState state )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = this.getItem( (TileComputerBase) tile );
            if( !result.isEmpty() )
            {
                return result;
            }
        }

        return super.getPickStack( world, pos, state );
    }

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    @Override
    public void onBreak( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull PlayerEntity player )
    {
        // Call super as it is what provides sound and block break particles. Does not do anything else.
        super.onBreak( world, pos, state, player );

        if( !(world instanceof ServerWorld) )
        {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) world;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            LootContext.Builder context = new LootContext.Builder( serverWorld ).random( world.random )
                .parameter( LootContextParameters.ORIGIN, Vec3d.ofCenter( pos ) )
                .parameter( LootContextParameters.TOOL, player.getMainHandStack() )
                .parameter( LootContextParameters.THIS_ENTITY, player )
                .parameter( LootContextParameters.BLOCK_ENTITY, tile )
                .putDrop( DROP, ( ctx, out ) -> out.accept( this.getItem( computer ) ) );
            for( ItemStack item : state.getDroppedStacks( context ) )
            {
                dropStack( world, pos, item );
            }

            state.onStacksDropped( serverWorld, pos, player.getMainHandStack() );
        }
    }
}
