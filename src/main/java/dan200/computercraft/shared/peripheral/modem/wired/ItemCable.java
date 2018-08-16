/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.ComputerCraft;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockCable.isCable;

public class ItemCable extends ItemBlock
{
    public ItemCable( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:cable" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items )
    {
        // Avoid ItemBlock logic
        if( isInCreativeTab( tab ) ) items.add( new ItemStack( this ) );
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUse( @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float fx, float fy, float fz )
    {
        ItemStack stack = player.getHeldItem( hand );
        if( stack.isEmpty() ) return EnumActionResult.FAIL;

        IBlockState existingState = world.getBlockState( pos );
        Block existing = existingState.getBlock();

        // Try to add a cable to a modem (clicking within the block)
        if( existing == ComputerCraft.Blocks.cable && !isCable( existingState ) )
        {
            IBlockState newState = existingState.withProperty( BlockCable.CABLE, BlockCableCableVariant.ANY );
            world.setBlockState( pos, newState, 3 );
            SoundType soundType = newState.getBlock().getSoundType( newState, world, pos, player );
            world.playSound( null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F );
            stack.shrink( 1 );

            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof TileCable )
            {
                TileCable cable = (TileCable) tile;
                cable.connectionsChanged();
            }

            return EnumActionResult.SUCCESS;
        }

        // Try to add a cable to a modem (clicking an adjacent block)
        if( !existing.isAir( existingState, world, pos ) )
        {
            BlockPos offset = pos.offset( side );
            IBlockState offsetExistingState = world.getBlockState( offset );
            Block offsetExisting = offsetExistingState.getBlock();
            if( offsetExisting == ComputerCraft.Blocks.cable && !isCable( offsetExistingState ) )
            {
                IBlockState newState = offsetExistingState.withProperty( BlockCable.CABLE, BlockCableCableVariant.ANY );
                world.setBlockState( offset, newState, 3 );
                SoundType soundType = newState.getBlock().getSoundType( newState, world, offset, player );
                world.playSound( null, offset.getX() + 0.5, offset.getY() + 0.5, offset.getZ() + 0.5, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F );
                stack.shrink( 1 );

                TileEntity tile = world.getTileEntity( offset );
                if( tile instanceof TileCable )
                {
                    TileCable cable = (TileCable) tile;
                    cable.modemChanged();
                    cable.connectionsChanged();
                }
                return EnumActionResult.SUCCESS;
            }
        }

        return super.onItemUse( player, world, pos, hand, side, fx, fy, fz );
    }

    @Override
    public boolean canPlaceBlockOnSide( World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, EntityPlayer player, ItemStack stack )
    {
        return world.isSideSolid( pos, side );
    }
}
