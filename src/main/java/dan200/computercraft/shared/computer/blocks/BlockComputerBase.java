/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
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
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private static final ResourceLocation DROP = new ResourceLocation( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;

    protected BlockComputerBase( Properties settings, ComputerFamily family, NamedTileEntityType<? extends T> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Override
    @Deprecated
    public void onBlockAdded( BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving )
    {
        super.onBlockAdded( state, world, pos, oldState, isMoving );

        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }

    @Override
    @Deprecated
    public boolean canProvidePower( BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getStrongPower( BlockState state, IBlockReader world, BlockPos pos, Direction incomingSide )
    {
        TileEntity entity = world.getTileEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
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
    public int getWeakPower( BlockState state, IBlockReader world, BlockPos pos, Direction incomingSide )
    {
        return getStrongPower( state, world, pos, incomingSide );
    }

    @Override
    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, Direction side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        TileEntity entity = world.getTileEntity( pos );
        if( !(entity instanceof TileComputerBase) ) return 0;

        TileComputerBase computerEntity = (TileComputerBase) entity;
        ServerComputer computer = computerEntity.getServerComputer();
        if( computer == null ) return 0;

        ComputerSide localSide = computerEntity.remapToLocalSide( side );
        return computer.getBundledRedstoneOutput( localSide );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player )
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
    public void harvestBlock( @Nonnull World world, PlayerEntity player, @Nonnull BlockPos pos, BlockState state, @Nullable TileEntity tile, @Nonnull ItemStack tool )
    {
        // Don't drop blocks here - see onBlockHarvested.
        player.addStat( Stats.BLOCK_MINED.get( this ) );
        player.addExhaustion( 0.005F );
    }

    @Override
    public void onBlockHarvested( World world, @Nonnull BlockPos pos, BlockState state, @Nonnull PlayerEntity player )
    {
        if( !(world instanceof ServerWorld) ) return;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            LootContext.Builder context = new LootContext.Builder( (ServerWorld) world )
                .withRandom( world.rand )
                .withParameter( LootParameters.POSITION, pos )
                .withParameter( LootParameters.TOOL, player.getHeldItemMainhand() )
                .withParameter( LootParameters.THIS_ENTITY, player )
                .withNullableParameter( LootParameters.BLOCK_ENTITY, tile )
                .withDynamicDrop( DROP, ( ctx, out ) -> out.accept( getItem( computer ) ) );
            for( ItemStack item : state.getDrops( context ) )
            {
                spawnAsEntity( world, pos, item );
            }

            state.spawnAdditionalDrops( world, pos, player.getHeldItemMainhand() );
        }
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack )
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
