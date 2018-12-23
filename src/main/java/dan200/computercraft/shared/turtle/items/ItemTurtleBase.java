/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputerBase;
import dan200.computercraft.shared.turtle.blocks.ITurtleTile;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ItemTurtleBase extends ItemComputerBase implements ITurtleItem
{
    protected ItemTurtleBase( Block block )
    {
        super( block );
        setMaxStackSize( 64 );
        setHasSubtypes( true );
    }

    public abstract ItemStack create( int id, String label, int colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, int fuelLevel, ResourceLocation overlay );

    @Override
    public void getSubItems( @Nullable CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        NonNullList<ItemStack> all = NonNullList.create();
        ComputerCraft.addAllUpgradedTurtles( all );
        for( ItemStack stack : all )
        {
            if( stack.getItem() == this )
            {
                list.add( stack );
            }
        }
    }

    @Override
    public boolean placeBlockAt( @Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState newState )
    {
        if( super.placeBlockAt( stack, player, world, pos, side, hitX, hitY, hitZ, newState ) )
        {
            TileEntity tile = world.getTileEntity( pos );
            if( tile != null && tile instanceof ITurtleTile )
            {
                ITurtleTile turtle = (ITurtleTile) tile;
                setupTurtleAfterPlacement( stack, turtle );
            }
            return true;
        }
        return false;
    }

    public void setupTurtleAfterPlacement( @Nonnull ItemStack stack, ITurtleTile turtle )
    {
        // Set ID
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            turtle.setComputerID( id );
        }

        // Set Label
        String label = getLabel( stack );
        if( label != null )
        {
            turtle.setLabel( label );
        }

        // Set Upgrades
        for( TurtleSide side : TurtleSide.values() )
        {
            turtle.getAccess().setUpgrade( side, getUpgrade( stack, side ) );
        }

        // Set Fuel level
        int fuelLevel = getFuelLevel( stack );
        turtle.getAccess().setFuelLevel( fuelLevel );

        // Set colour
        int colour = getColour( stack );
        if( colour != -1 )
        {
            turtle.getAccess().setColour( colour );
        }

        // Set overlay
        ResourceLocation overlay = getOverlay( stack );
        if( overlay != null )
        {
            ((TurtleBrain) turtle.getAccess()).setOverlay( overlay );
        }
    }

    @Nonnull
    @Override
    public String getTranslationKey( @Nonnull ItemStack stack )
    {
        ComputerFamily family = getFamily( stack );
        switch( family )
        {
            case Normal:
            default:
            {
                return "tile.computercraft:turtle";
            }
            case Advanced:
            {
                return "tile.computercraft:advanced_turtle";
            }
            case Beginners:
            {
                return "tile.computercraftedu:beginner_turtle";
            }
        }
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        ITurtleUpgrade left = getUpgrade( stack, TurtleSide.Left );
        ITurtleUpgrade right = getUpgrade( stack, TurtleSide.Right );
        if( left != null && right != null )
        {
            return StringUtil.translateToLocalFormatted(
                baseString + ".upgraded_twice.name",
                StringUtil.translateToLocal( right.getUnlocalisedAdjective() ),
                StringUtil.translateToLocal( left.getUnlocalisedAdjective() )
            );
        }
        else if( left != null )
        {
            return StringUtil.translateToLocalFormatted(
                baseString + ".upgraded.name",
                StringUtil.translateToLocal( left.getUnlocalisedAdjective() )
            );
        }
        else if( right != null )
        {
            return StringUtil.translateToLocalFormatted(
                baseString + ".upgraded.name",
                StringUtil.translateToLocal( right.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return StringUtil.translateToLocal( baseString + ".name" );
        }
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return TurtleItemFactory.create(
            getComputerID( stack ), getLabel( stack ),
            getColour( stack ), family,
            getUpgrade( stack, TurtleSide.Left ), getUpgrade( stack, TurtleSide.Right ),
            getFuelLevel( stack ), getOverlay( stack )
        );
    }

    @Override
    public ItemStack setColour( ItemStack stack, int colour )
    {
        return TurtleItemFactory.create(
            getComputerID( stack ), getLabel( stack ), colour, getFamily( stack ),
            getUpgrade( stack, TurtleSide.Left ), getUpgrade( stack, TurtleSide.Right ),
            getFuelLevel( stack ), getOverlay( stack )
        );
    }
}
