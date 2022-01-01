/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private static final ResourceLocation DROP = new ResourceLocation( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;
    protected final RegistryObject<BlockEntityType<T>> type;
    private final BlockEntityTicker<T> serverTicker = ( level, pos, state, computer ) -> computer.serverTick();

    protected BlockComputerBase( Properties settings, ComputerFamily family, RegistryObject<BlockEntityType<T>> type )
    {
        super( settings, type );
        this.family = family;
        this.type = type;
    }

    @Override
    @Deprecated
    public void onPlace( @Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving )
    {
        super.onPlace( state, world, pos, oldState, isMoving );

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase computer ) computer.updateInputsImmediately();
    }

    @Override
    @Deprecated
    public boolean isSignalSource( @Nonnull BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getDirectSignal( @Nonnull BlockState state, BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        BlockEntity entity = world.getBlockEntity( pos );
        if( !(entity instanceof TileComputerBase computerEntity) ) return 0;

        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        ComputerSide localSide = computerEntity.remapToLocalSide( incomingSide.getOpposite() );
        return computer.getRedstoneOutput( localSide );
    }

    @Nonnull
    protected abstract ItemStack getItem( TileComputerBase tile );

    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    @Deprecated
    public int getSignal( @Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        return getDirectSignal( state, world, pos, incomingSide );
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
        if( !(entity instanceof TileComputerBase computerEntity) ) return 0;

        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        ComputerSide localSide = computerEntity.remapToLocalSide( side );
        return computer.getBundledRedstoneOutput( localSide );
    }

    @Nonnull
    @Override
    public ItemStack getCloneItemStack( BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() ) return result;
        }

        return super.getCloneItemStack( state, target, world, pos, player );
    }

    @Override
    public void playerDestroy( @Nonnull Level world, Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity tile, @Nonnull ItemStack tool )
    {
        // Don't drop blocks here - see onBlockHarvested.
        player.awardStat( Stats.BLOCK_MINED.get( this ) );
        player.causeFoodExhaustion( 0.005F );
    }

    @Override
    public void playerWillDestroy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Player player )
    {
        if( !(world instanceof ServerLevel serverWorld) ) return;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase computer )
        {
            LootContext.Builder context = new LootContext.Builder( serverWorld )
                .withRandom( world.random )
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

    @Override
    public void setPlacedBy( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        super.setPlacedBy( world, pos, state, placer, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClientSide && tile instanceof IComputerTile computer && stack.getItem() instanceof IComputerItem item )
        {

            int id = item.getComputerID( stack );
            if( id != -1 ) computer.setComputerID( id );

            String label = item.getLabel( stack );
            if( label != null ) computer.setLabel( label );
        }
    }

    @Override
    public boolean shouldCheckWeakPower( BlockState state, LevelReader world, BlockPos pos, Direction side )
    {
        return false;
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker( @Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<U> type )
    {
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper( type, this.type.get(), serverTicker );
    }
}
