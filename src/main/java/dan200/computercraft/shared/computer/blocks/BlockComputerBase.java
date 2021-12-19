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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private static final ResourceLocation DROP = new ResourceLocation( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;

    protected BlockComputerBase( Properties settings, ComputerFamily family, Supplier<BlockEntityType<? extends T>> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Nonnull
    @Override
    @SuppressWarnings( "unchecked" )
    public BlockEntityType<? extends T> getType()
    {
        return (BlockEntityType<? extends T>) super.getType();
    }

    @Override
    @Deprecated
    public void onPlace( @Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving )
    {
        super.onPlace( state, world, pos, oldState, isMoving );

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ((TileComputerBase) tile).updateInput();
        }
    }

    @Override
    @Deprecated
    public boolean isSignalSource( @Nonnull BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getSignal( @Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        return getDirectSignal( state, world, pos, incomingSide );
    }

    @Override
    @Deprecated
    public int getDirectSignal( @Nonnull BlockState state, BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
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
        return family;
    }

    @Override
    public boolean getBundledRedstoneConnectivity( Level world, BlockPos pos, Direction side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( Level world, BlockPos pos, Direction side )
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
    public void playerDestroy( @Nonnull Level world, Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity tile,
                               @Nonnull ItemStack tool )
    {
        // Don't drop blocks here - see onBlockHarvested.
        player.awardStat( Stats.BLOCK_MINED.get( this ) );
        player.causeFoodExhaustion( 0.005F );
    }

    @Override
    public void setPlacedBy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        super.setPlacedBy( world, pos, state, placer, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClientSide && tile instanceof IComputerTile && stack.getItem() instanceof IComputerItem )
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
    public ItemStack getCloneItemStack( BlockGetter world, BlockPos pos, BlockState state )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() )
            {
                return result;
            }
        }

        return super.getCloneItemStack( world, pos, state );
    }

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    @Override
    public void playerWillDestroy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Player player )
    {
        // Call super as it is what provides sound and block break particles. Does not do anything else.
        super.playerWillDestroy( world, pos, state, player );

        if( !(world instanceof ServerLevel) )
        {
            return;
        }
        ServerLevel serverWorld = (ServerLevel) world;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            LootContext.Builder context = new LootContext.Builder( serverWorld ).withRandom( world.random )
                .withParameter( LootContextParams.ORIGIN, Vec3.atCenterOf( pos ) )
                .withParameter( LootContextParams.TOOL, player.getMainHandItem() )
                .withParameter( LootContextParams.THIS_ENTITY, player )
                .withParameter( LootContextParams.BLOCK_ENTITY, tile )
                .withDynamicDrop( DROP, ( ctx, out ) -> out.accept( getItem( computer ) ) );
            for( ItemStack item : state.getDrops( context ) )
            {
                popResource( world, pos, item );
            }

            state.spawnAfterBreak( serverWorld, pos, player.getMainHandItem() );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker( Level world, BlockState state, BlockEntityType<T> type )
    {
        return world.isClientSide ? null : ( world1, pos, state1, tile ) -> {
            if( tile instanceof TileComputerBase computer )
            {
                computer.serverTick();
            }
        };
    }
}
