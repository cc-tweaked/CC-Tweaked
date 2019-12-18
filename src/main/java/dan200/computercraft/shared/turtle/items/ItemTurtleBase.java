/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputerBase;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

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

    public abstract ComputerFamily getFamily();

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return getFamily();
    }

    @Override
    public void getSubItems( @Nullable CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;

        ComputerFamily family = getFamily();

        ItemStack normalStack = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
        if( !normalStack.isEmpty() && normalStack.getItem() == this ) list.add( normalStack );

        TurtleUpgrades.getVanillaUpgrades()
            .filter( x -> TurtleUpgrades.suitableForFamily( family, x ) )
            .map( x -> TurtleItemFactory.create( -1, null, -1, family, null, x, 0, null ) )
            .filter( x -> !x.isEmpty() && x.getItem() == this )
            .forEach( list::add );
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
                return "tile.computercraft:turtle";
            case Advanced:
                return "tile.computercraft:advanced_turtle";
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
            return StringUtil.translateFormatted(
                baseString + ".upgraded_twice.name",
                StringUtil.translate( right.getUnlocalisedAdjective() ),
                StringUtil.translate( left.getUnlocalisedAdjective() )
            );
        }
        else if( left != null )
        {
            return StringUtil.translateFormatted(
                baseString + ".upgraded.name",
                StringUtil.translate( left.getUnlocalisedAdjective() )
            );
        }
        else if( right != null )
        {
            return StringUtil.translateFormatted(
                baseString + ".upgraded.name",
                StringUtil.translate( right.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return StringUtil.translate( baseString + ".name" );
        }
    }

    @Nullable
    @Override
    public String getCreatorModId( ItemStack stack )
    {
        // Determine our "creator mod" from the upgrades. We attempt to find the first non-vanilla/non-CC
        // upgrade (starting from the left).

        ITurtleUpgrade left = getUpgrade( stack, TurtleSide.Left );
        if( left != null )
        {
            String mod = TurtleUpgrades.getOwner( left );
            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
        }

        ITurtleUpgrade right = getUpgrade( stack, TurtleSide.Right );
        if( right != null )
        {
            String mod = TurtleUpgrades.getOwner( right );
            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
        }

        return super.getCreatorModId( stack );
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
    public ItemStack withColour( ItemStack stack, int colour )
    {
        return TurtleItemFactory.create(
            getComputerID( stack ), getLabel( stack ), colour, getFamily( stack ),
            getUpgrade( stack, TurtleSide.Left ), getUpgrade( stack, TurtleSide.Right ),
            getFuelLevel( stack ), getOverlay( stack )
        );
    }
}
