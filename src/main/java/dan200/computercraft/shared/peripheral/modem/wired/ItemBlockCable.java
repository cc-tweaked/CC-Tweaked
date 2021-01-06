/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.shared.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.*;

public abstract class ItemBlockCable extends BlockItem
{
    private String translationKey;

    public ItemBlockCable( BlockCable block, Properties settings )
    {
        super( block, settings );
    }

    boolean placeAt( World world, BlockPos pos, BlockState state, PlayerEntity player )
    {
        // TODO: Check entity collision.
        if( !state.isValidPosition( world, pos ) ) return false;

        world.setBlockState( pos, state, 3 );
        SoundType soundType = state.getBlock().getSoundType( state, world, pos, player );
        world.playSound( null, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F );

        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            cable.modemChanged();
            cable.connectionsChanged();
        }

        return true;
    }

    boolean placeAtCorrected( World world, BlockPos pos, BlockState state )
    {
        return placeAt( world, pos, correctConnections( world, pos, state ), null );
    }

    @Override
    public void fillItemGroup( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> list )
    {
        if( isInGroup( group ) ) list.add( new ItemStack( this ) );
    }

    @Nonnull
    @Override
    public String getTranslationKey()
    {
        if( translationKey == null )
        {
            translationKey = Util.makeTranslationKey( "block", ForgeRegistries.ITEMS.getKey( this ) );
        }
        return translationKey;
    }

    public static class WiredModem extends ItemBlockCable
    {
        public WiredModem( BlockCable block, Properties settings )
        {
            super( block, settings );
        }

        @Nonnull
        @Override
        public ActionResultType tryPlace( BlockItemUseContext context )
        {
            ItemStack stack = context.getItem();
            if( stack.isEmpty() ) return ActionResultType.FAIL;

            World world = context.getWorld();
            BlockPos pos = context.getPos();
            BlockState existingState = world.getBlockState( pos );

            // Try to add a modem to a cable
            if( existingState.getBlock() == Registry.ModBlocks.CABLE.get() && existingState.get( MODEM ) == CableModemVariant.None )
            {
                Direction side = context.getFace().getOpposite();
                BlockState newState = existingState
                    .with( MODEM, CableModemVariant.from( side ) )
                    .with( CONNECTIONS.get( side ), existingState.get( CABLE ) );
                if( placeAt( world, pos, newState, context.getPlayer() ) )
                {
                    stack.shrink( 1 );
                    return ActionResultType.SUCCESS;
                }
            }

            return super.tryPlace( context );
        }
    }

    public static class Cable extends ItemBlockCable
    {
        public Cable( BlockCable block, Properties settings )
        {
            super( block, settings );
        }

        @Nonnull
        @Override
        public ActionResultType tryPlace( BlockItemUseContext context )
        {
            ItemStack stack = context.getItem();
            if( stack.isEmpty() ) return ActionResultType.FAIL;

            World world = context.getWorld();
            BlockPos pos = context.getPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            BlockPos insidePos = pos.offset( context.getFace().getOpposite() );
            BlockState insideState = world.getBlockState( insidePos );
            if( insideState.getBlock() == Registry.ModBlocks.CABLE.get() && !insideState.get( BlockCable.CABLE )
                && placeAtCorrected( world, insidePos, insideState.with( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return ActionResultType.SUCCESS;
            }

            // Try to add a cable to a modem adjacent to this block
            BlockState existingState = world.getBlockState( pos );
            if( existingState.getBlock() == Registry.ModBlocks.CABLE.get() && !existingState.get( BlockCable.CABLE )
                && placeAtCorrected( world, pos, existingState.with( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return ActionResultType.SUCCESS;
            }

            return super.tryPlace( context );
        }
    }
}
