/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.ComputerCraft;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.*;

public abstract class ItemBlockCable extends ItemBlock
{
    private String translationKey;

    public ItemBlockCable( BlockCable block, Properties settings )
    {
        super( block, settings );
    }

    boolean placeAt( World world, BlockPos pos, IBlockState state, EntityPlayer player )
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

    boolean placeAtCorrected( World world, BlockPos pos, IBlockState state )
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
        public EnumActionResult tryPlace( BlockItemUseContext context )
        {
            ItemStack stack = context.getItem();
            if( stack.isEmpty() ) return EnumActionResult.FAIL;

            World world = context.getWorld();
            BlockPos pos = context.getPos();
            IBlockState existingState = world.getBlockState( pos );

            // Try to add a modem to a cable
            if( existingState.getBlock() == ComputerCraft.Blocks.cable && existingState.get( MODEM ) == CableModemVariant.None )
            {
                EnumFacing side = context.getFace().getOpposite();
                IBlockState newState = existingState
                    .with( MODEM, CableModemVariant.from( side ) )
                    .with( CONNECTIONS.get( side ), existingState.get( CABLE ) );
                if( placeAt( world, pos, newState, context.getPlayer() ) )
                {
                    stack.shrink( 1 );
                    return EnumActionResult.SUCCESS;
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
        public EnumActionResult tryPlace( BlockItemUseContext context )
        {
            ItemStack stack = context.getItem();
            if( stack.isEmpty() ) return EnumActionResult.FAIL;

            World world = context.getWorld();
            BlockPos pos = context.getPos();

            // Try to add a cable to a modem inside the block we're clicking on.
            BlockPos insidePos = pos.offset( context.getFace().getOpposite() );
            IBlockState insideState = world.getBlockState( insidePos );
            if( insideState.getBlock() == ComputerCraft.Blocks.cable && !insideState.get( BlockCable.CABLE )
                && placeAtCorrected( world, insidePos, insideState.with( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return EnumActionResult.SUCCESS;
            }

            // Try to add a cable to a modem adjacent to this block
            IBlockState existingState = world.getBlockState( pos );
            if( existingState.getBlock() == ComputerCraft.Blocks.cable && !existingState.get( BlockCable.CABLE )
                && placeAtCorrected( world, pos, existingState.with( BlockCable.CABLE, true ) ) )
            {
                stack.shrink( 1 );
                return EnumActionResult.SUCCESS;
            }

            return super.tryPlace( context );
        }
    }
}
