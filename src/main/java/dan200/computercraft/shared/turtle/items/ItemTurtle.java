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
import dan200.computercraft.shared.turtle.blocks.ITurtleTile;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

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
        if( label != null ) stack.setDisplayName( new TextComponentString( label ) );
        if( id >= 0 ) stack.getOrCreateTag().putInt( NBT_ID, id );
        IColouredItem.setColourBasic( stack, colour );
        if( fuelLevel > 0 ) stack.getOrCreateTag().putInt( NBT_FUEL, fuelLevel );
        if( overlay != null ) stack.getOrCreateTag().putString( NBT_OVERLAY, overlay.toString() );

        if( leftUpgrade != null )
        {
            stack.getOrCreateTag().putString( NBT_LEFT_UPGRADE, leftUpgrade.getUpgradeId().toString() );
        }

        if( rightUpgrade != null )
        {
            stack.getOrCreateTag().putString( NBT_RIGHT_UPGRADE, rightUpgrade.getUpgradeId().toString() );
        }

        return stack;
    }

    @Override
    public void fillItemGroup( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInGroup( group ) ) return;

        ComputerFamily family = getFamily();

        list.add( create( -1, null, -1, null, null, 0, null ) );
        for( ITurtleUpgrade upgrade : TurtleUpgrades.getVanillaUpgrades() )
        {
            if( !TurtleUpgrades.suitableForFamily( family, upgrade ) ) continue;

            list.add( create( -1, null, -1, upgrade, null, 0, null ) );
        }
    }

    @Override
    protected boolean onBlockPlaced( @Nonnull BlockPos pos, World world, @Nullable EntityPlayer player, @Nonnull ItemStack stack, IBlockState state )
    {
        boolean changed = super.onBlockPlaced( pos, world, player, stack, state );

        TileEntity entity = world.getTileEntity( pos );
        if( !world.isRemote && entity instanceof ITurtleTile )
        {
            ITurtleTile turtle = (ITurtleTile) entity;
            setupTurtleAfterPlacement( stack, turtle );
            changed = true;
        }

        return changed;
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

    @Override
    public ITextComponent getDisplayName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        ITurtleUpgrade left = getUpgrade( stack, TurtleSide.Left );
        ITurtleUpgrade right = getUpgrade( stack, TurtleSide.Right );
        if( left != null && right != null )
        {
            return new TextComponentTranslation( baseString + ".upgraded_twice",
                new TextComponentTranslation( right.getUnlocalisedAdjective() ),
                new TextComponentTranslation( left.getUnlocalisedAdjective() )
            );
        }
        else if( left != null )
        {
            return new TextComponentTranslation( baseString + ".upgraded",
                new TextComponentTranslation( left.getUnlocalisedAdjective() )
            );
        }
        else if( right != null )
        {
            return new TextComponentTranslation( baseString + ".upgraded",
                new TextComponentTranslation( right.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return new TextComponentTranslation( baseString );
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
    public ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, TurtleSide side )
    {
        NBTTagCompound tag = stack.getTag();
        if( tag == null ) return null;

        String key = side == TurtleSide.Left ? NBT_LEFT_UPGRADE : NBT_RIGHT_UPGRADE;
        return tag.contains( key ) ? TurtleUpgrades.get( tag.getString( key ) ) : null;
    }

    @Override
    public ResourceLocation getOverlay( @Nonnull ItemStack stack )
    {
        NBTTagCompound tag = stack.getTag();
        return tag != null && tag.contains( NBT_OVERLAY ) ? new ResourceLocation( tag.getString( NBT_OVERLAY ) ) : null;
    }

    @Override
    public int getFuelLevel( @Nonnull ItemStack stack )
    {
        NBTTagCompound tag = stack.getTag();
        return tag != null && tag.contains( NBT_FUEL ) ? tag.getInt( NBT_FUEL ) : 0;
    }
}
