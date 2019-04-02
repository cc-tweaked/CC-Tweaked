/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemPeripheralBase;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemCable extends ItemPeripheralBase
{
    public ItemCable( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:cable" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setHasSubtypes( true );
    }

    @Nonnull
    public ItemStack create( PeripheralType type, int quantity )
    {
        switch( type )
        {
            case Cable:
                return new ItemStack( this, quantity, 0 );
            case WiredModem:
                return new ItemStack( this, quantity, 1 );
            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public void getSubItems( @Nullable CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        list.add( PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 ) );
        list.add( PeripheralItemFactory.create( PeripheralType.Cable, null, 1 ) );
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUse( @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float fx, float fy, float fz )
    {
        ItemStack stack = player.getHeldItem( hand );
        if( !canPlaceBlockOnSide( world, pos, side, player, stack ) ) return EnumActionResult.FAIL;

        // Try to add a cable to a modem
        PeripheralType type = getPeripheralType( stack );
        IBlockState existingState = world.getBlockState( pos );
        Block existing = existingState.getBlock();
        if( existing == ComputerCraft.Blocks.cable )
        {
            PeripheralType existingType = BlockCable.getPeripheralType( existingState );
            if( existingType == PeripheralType.WiredModem && type == PeripheralType.Cable )
            {
                if( !stack.isEmpty() )
                {
                    IBlockState newState = existingState.withProperty( BlockCable.CABLE, true );
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
                return EnumActionResult.FAIL;
            }
        }

        // Try to add on the side of something
        if( !existing.isAir( existingState, world, pos ) && (type == PeripheralType.Cable || existingState.isSideSolid( world, pos, side )) )
        {
            BlockPos offset = pos.offset( side );
            IBlockState offsetExistingState = world.getBlockState( offset );
            Block offsetExisting = offsetExistingState.getBlock();
            if( offsetExisting == ComputerCraft.Blocks.cable )
            {
                // Try to add a modem to a cable
                PeripheralType offsetExistingType = BlockCable.getPeripheralType( offsetExistingState );
                if( offsetExistingType == PeripheralType.Cable && type == PeripheralType.WiredModem )
                {
                    if( !stack.isEmpty() )
                    {
                        IBlockState newState = offsetExistingState.withProperty( BlockCable.MODEM, BlockCableModemVariant.from( side.getOpposite() ) );
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
                    return EnumActionResult.FAIL;
                }

                // Try to add a cable to a modem
                if( offsetExistingType == PeripheralType.WiredModem && type == PeripheralType.Cable )
                {
                    if( !stack.isEmpty() )
                    {
                        IBlockState newState = offsetExistingState.withProperty( BlockCable.CABLE, true );
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
                    return EnumActionResult.FAIL;
                }
            }
        }

        return super.onItemUse( player, world, pos, hand, side, fx, fy, fz );
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return damage == 1 ? PeripheralType.WiredModem : PeripheralType.Cable;
    }
}
