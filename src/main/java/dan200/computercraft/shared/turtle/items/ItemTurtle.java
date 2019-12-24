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
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputerBase;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.turtle.core.TurtleBrain.*;

public class ItemTurtle extends ItemComputerBase implements ITurtleItem
{
    public ItemTurtle( BlockTurtle block, Properties settings )
    {
        super( block, settings );
    }

    public ItemStack create( int id, String label, int colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, int fuelLevel, ResourceLocation overlay )
    {
        // Build the stack
        ItemStack stack = new ItemStack( this );
        if( label != null ) stack.setDisplayName( new StringTextComponent( label ) );
        if( id >= 0 ) stack.getOrCreateTag().putInt( NBT_ID, id );
        IColouredItem.setColourBasic( stack, colour );
        if( fuelLevel > 0 ) stack.getOrCreateTag().putInt( NBT_FUEL, fuelLevel );
        if( overlay != null ) stack.getOrCreateTag().putString( NBT_OVERLAY, overlay.toString() );

        if( leftUpgrade != null )
        {
            stack.getOrCreateTag().putString( NBT_LEFT_UPGRADE, leftUpgrade.getUpgradeID().toString() );
        }

        if( rightUpgrade != null )
        {
            stack.getOrCreateTag().putString( NBT_RIGHT_UPGRADE, rightUpgrade.getUpgradeID().toString() );
        }

        return stack;
    }

    @Override
    public void fillItemGroup( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInGroup( group ) ) return;

        ComputerFamily family = getFamily();

        list.add( create( -1, null, -1, null, null, 0, null ) );
        TurtleUpgrades.getVanillaUpgrades()
            .filter( x -> TurtleUpgrades.suitableForFamily( family, x ) )
            .map( x -> create( -1, null, -1, null, x, 0, null ) )
            .forEach( list::add );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        ITurtleUpgrade left = getUpgrade( stack, TurtleSide.Left );
        ITurtleUpgrade right = getUpgrade( stack, TurtleSide.Right );
        if( left != null && right != null )
        {
            return new TranslationTextComponent( baseString + ".upgraded_twice",
                new TranslationTextComponent( right.getUnlocalisedAdjective() ),
                new TranslationTextComponent( left.getUnlocalisedAdjective() )
            );
        }
        else if( left != null )
        {
            return new TranslationTextComponent( baseString + ".upgraded",
                new TranslationTextComponent( left.getUnlocalisedAdjective() )
            );
        }
        else if( right != null )
        {
            return new TranslationTextComponent( baseString + ".upgraded",
                new TranslationTextComponent( right.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return new TranslationTextComponent( baseString );
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
    public ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, @Nonnull TurtleSide side )
    {
        CompoundNBT tag = stack.getTag();
        if( tag == null ) return null;

        String key = side == TurtleSide.Left ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        return tag.contains( key ) ? TurtleUpgrades.get( tag.getString( key ) ) : null;
    }

    @Override
    public ResourceLocation getOverlay( @Nonnull ItemStack stack )
    {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains( NBT_OVERLAY ) ? new ResourceLocation( tag.getString( NBT_OVERLAY ) ) : null;
    }

    @Override
    public int getFuelLevel( @Nonnull ItemStack stack )
    {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains( NBT_FUEL ) ? tag.getInt( NBT_FUEL ) : 0;
    }
}
