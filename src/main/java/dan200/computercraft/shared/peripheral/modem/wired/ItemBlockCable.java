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
        if( !state.canSurvive( world, pos ) ) return false;

        world.setBlock( pos, state, 3 );
        SoundType soundType = state.getBlock().getSoundType( state, world, pos, player );
        world.playSound( null, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F );

        TileEntity tile = world.getBlockEntity( pos );
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
    public void fillItemCategory( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> list )
    {
        if( allowdedIn( group ) ) list.add( new ItemStack( this ) );
    }

    @Nonnull
    @Override
    public String getDescriptionId()
    {
        if( translationKey == null )
        {
            translationKey = Util.makeDescriptionId( "block", ForgeRegistries.ITEMS.getKey( this ) );
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
        public ActionResultType place( BlockItemUseContext context )
        {
            ItemStack stack = context.getItemInHand();
            if( stack.isEmpty() ) return ActionResultType.FAIL;

            World world = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState existingState = world.getBlockState( pos );

            // Try to add a modem to a cable
            if( existingState.getBlock() == Registry.ModBlocks.CABLE.get() && existingState.getValue( MODEM ) == CableModemVariant.None )
            {
                Direction side = context.getClickedFace().getOpposite();
                BlockState newState = existingState
                    .setValue( MODEM, CableModemVariant.from( side ) )
                    .setValue( CONNECTIONS.get( side ), existingState.getValue( CABLE ) );
                if( placeAt( world, pos, newState, context.getPlayer() ) )
                {
                    stack.shrink( 1 );
                    return ActionResultType.SUCCESS;
                }
            }

            return super.place( context );
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
        public ActionResultType place( BlockItemUseContext context )
        {
            ItemStack stack = context.getItemInHand();
            if( stack.isEmpty() ) return ActionResultType.FAIL;

            World world = context.getLevel();
            BlockPos pos = context.getClickedPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            BlockPos insidePos = pos.relative( context.getClickedFace().getOpposite() );
            BlockState insideState = world.getBlockState( insidePos );
            if( insideState.getBlock() == Registry.ModBlocks.CABLE.get() && !insideState.getValue( BlockCable.CABLE )
                && placeAtCorrected( world, insidePos, insideState.setValue( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return ActionResultType.SUCCESS;
            }

            // Try to add a cable to a modem adjacent to this block
            BlockState existingState = world.getBlockState( pos );
            if( existingState.getBlock() == Registry.ModBlocks.CABLE.get() && !existingState.getValue( BlockCable.CABLE )
                && placeAtCorrected( world, pos, existingState.setValue( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return ActionResultType.SUCCESS;
            }

            return super.place( context );
        }
    }
}
