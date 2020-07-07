/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ItemComputerBase;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import javax.annotation.Nonnull;

import static dan200.computercraft.shared.turtle.core.TurtleBrain.*;

public class ItemTurtle extends ItemComputerBase implements ITurtleItem
{
    public ItemTurtle( BlockTurtle block, Settings settings )
    {
        super( block, settings );
    }

    public ItemStack create( int id, String label, int colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, int fuelLevel, Identifier overlay )
    {
        // Build the stack
        ItemStack stack = new ItemStack( this );
        if( label != null ) stack.setCustomName( new LiteralText( label ) );
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
    public void appendStacks( @Nonnull ItemGroup group, @Nonnull DefaultedList<ItemStack> list )
    {
        if( !isIn( group ) ) return;

        ComputerFamily family = getFamily();

        list.add( create( -1, null, -1, null, null, 0, null ) );
        for( ITurtleUpgrade upgrade : TurtleUpgrades.getVanillaUpgrades() )
        {
            if( !TurtleUpgrades.suitableForFamily( family, upgrade ) ) continue;

            list.add( create( -1, null, -1, null, upgrade, 0, null ) );
        }
    }

    @Nonnull
    @Override
    public Text getName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        ITurtleUpgrade left = getUpgrade( stack, TurtleSide.Left );
        ITurtleUpgrade right = getUpgrade( stack, TurtleSide.Right );
        if( left != null && right != null )
        {
            return new TranslatableText( baseString + ".upgraded_twice",
                new TranslatableText( right.getUnlocalisedAdjective() ),
                new TranslatableText( left.getUnlocalisedAdjective() )
            );
        }
        else if( left != null )
        {
            return new TranslatableText( baseString + ".upgraded",
                new TranslatableText( left.getUnlocalisedAdjective() )
            );
        }
        else if( right != null )
        {
            return new TranslatableText( baseString + ".upgraded",
                new TranslatableText( right.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return new TranslatableText( baseString );
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
    public ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, @Nonnull TurtleSide side )
    {
        CompoundTag tag = stack.getTag();
        if( tag == null ) return null;

        String key = side == TurtleSide.Left ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        return tag.contains( key ) ? TurtleUpgrades.get( tag.getString( key ) ) : null;
    }

    @Override
    public Identifier getOverlay( @Nonnull ItemStack stack )
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains( NBT_OVERLAY ) ? new Identifier( tag.getString( NBT_OVERLAY ) ) : null;
    }

    @Override
    public int getFuelLevel( @Nonnull ItemStack stack )
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains( NBT_FUEL ) ? tag.getInt( NBT_FUEL ) : 0;
    }
}
