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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock
{
    private static final ResourceLocation DROP = new ResourceLocation( ComputerCraft.MOD_ID, "computer" );

    private final ComputerFamily family;

    protected BlockComputerBase( Properties settings, ComputerFamily family, RegistryObject<? extends TileEntityType<? extends T>> type )
    {
        super( settings, type );
        this.family = family;
    }

    @Override
    @Deprecated
    public void onPlace( @Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving )
    {
        super.onPlace( state, world, pos, oldState, isMoving );

        TileEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase ) ((TileComputerBase) tile).updateInput();
    }

    @Override
    @Deprecated
    public boolean isSignalSource( @Nonnull BlockState state )
    {
        return true;
    }

    @Override
    @Deprecated
    public int getDirectSignal( @Nonnull BlockState state, IBlockReader world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        TileEntity entity = world.getBlockEntity( pos );
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
    public int getSignal( @Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull Direction incomingSide )
    {
        return getDirectSignal( state, world, pos, incomingSide );
    }

    @Override
    public boolean getBundledRedstoneConnectivity( World world, BlockPos pos, Direction side )
    {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        TileEntity entity = world.getBlockEntity( pos );
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
        TileEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            ItemStack result = getItem( (TileComputerBase) tile );
            if( !result.isEmpty() ) return result;
        }

        return super.getPickBlock( state, target, world, pos, player );
    }

    @Override
    public void playerDestroy( @Nonnull World world, PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable TileEntity tile, @Nonnull ItemStack tool )
    {
        // Don't drop blocks here - see onBlockHarvested.
        player.awardStat( Stats.BLOCK_MINED.get( this ) );
        player.causeFoodExhaustion( 0.005F );
    }

    @Override
    public void playerWillDestroy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull PlayerEntity player )
    {
        if( !(world instanceof ServerWorld) ) return;
        ServerWorld serverWorld = (ServerWorld) world;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        TileEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileComputerBase )
        {
            TileComputerBase computer = (TileComputerBase) tile;
            LootContext.Builder context = new LootContext.Builder( serverWorld )
                .withRandom( world.random )
                .withParameter( LootParameters.ORIGIN, Vector3d.atCenterOf( pos ) )
                .withParameter( LootParameters.TOOL, player.getMainHandItem() )
                .withParameter( LootParameters.THIS_ENTITY, player )
                .withParameter( LootParameters.BLOCK_ENTITY, tile )
                .withDynamicDrop( DROP, ( ctx, out ) -> out.accept( getItem( computer ) ) );
            for( ItemStack item : state.getDrops( context ) )
            {
                popResource( world, pos, item );
            }

            state.spawnAfterBreak( serverWorld, pos, player.getMainHandItem() );
        }
    }

    @Override
    public void setPlacedBy( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, LivingEntity placer, @Nonnull ItemStack stack )
    {
        super.setPlacedBy( world, pos, state, placer, stack );

        TileEntity tile = world.getBlockEntity( pos );
        if( !world.isClientSide && tile instanceof IComputerTile && stack.getItem() instanceof IComputerItem )
        {
            IComputerTile computer = (IComputerTile) tile;
            IComputerItem item = (IComputerItem) stack.getItem();

            int id = item.getComputerID( stack );
            if( id != -1 ) computer.setComputerID( id );

            String label = item.getLabel( stack );
            if( label != null ) computer.setLabel( label );
        }
    }

    @Override
    public boolean shouldCheckWeakPower( BlockState state, IWorldReader world, BlockPos pos, Direction side )
    {
        return false;
    }
}
