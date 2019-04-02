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
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;

public class ItemTurtleNormal extends ItemTurtleBase
{
    public ItemTurtleNormal( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:turtle" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    public ItemStack create( int id, String label, int colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, int fuelLevel, ResourceLocation overlay )
    {
        ItemStack stack = new ItemStack( this, 1, 0 );
        NBTTagCompound nbt = new NBTTagCompound();
        if( leftUpgrade != null ) nbt.setString( "leftUpgrade", leftUpgrade.getUpgradeID().toString() );
        if( rightUpgrade != null ) nbt.setString( "rightUpgrade", rightUpgrade.getUpgradeID().toString() );
        if( id >= 0 ) nbt.setInteger( "computerID", id );
        if( fuelLevel > 0 ) nbt.setInteger( "fuelLevel", fuelLevel );
        if( colour != -1 ) nbt.setInteger( "colour", colour );
        if( overlay != null )
        {
            nbt.setString( "overlay_mod", overlay.getNamespace() );
            nbt.setString( "overlay_path", overlay.getPath() );
        }
        stack.setTagCompound( nbt );

        if( label != null ) stack.setStackDisplayName( label );

        return stack;
    }

    // IComputerItem implementation

    @Override
    public int getComputerID( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "computerID" ) ? nbt.getInteger( "computerID" ) : -1;
    }

    @Override
    public ComputerFamily getFamily()
    {
        return ComputerFamily.Normal;
    }

    // ITurtleItem implementation

    @Override
    public ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, @Nonnull TurtleSide side )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        if( nbt == null ) return null;
        switch( side )
        {
            case Left:
                if( nbt.hasKey( "leftUpgrade" ) )
                {
                    return nbt.getTagId( "leftUpgrade" ) == Constants.NBT.TAG_STRING
                        ? TurtleUpgrades.get( nbt.getString( "leftUpgrade" ) )
                        : TurtleUpgrades.get( nbt.getShort( "leftUpgrade" ) );
                }
                break;
            case Right:
                if( nbt.hasKey( "rightUpgrade" ) )
                {
                    return nbt.getTagId( "rightUpgrade" ) == Constants.NBT.TAG_STRING
                        ? TurtleUpgrades.get( nbt.getString( "rightUpgrade" ) )
                        : TurtleUpgrades.get( nbt.getShort( "rightUpgrade" ) );
                }
                break;
        }
        return null;
    }

    @Override
    public int getColour( @Nonnull ItemStack stack )
    {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? -1 : ColourUtils.getHexColour( tag );
    }

    @Override
    public ResourceLocation getOverlay( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        if( nbt != null && nbt.hasKey( "overlay_mod" ) && nbt.hasKey( "overlay_path" ) )
        {
            String overlayMod = nbt.getString( "overlay_mod" );
            String overlayPath = nbt.getString( "overlay_path" );
            return new ResourceLocation( overlayMod, overlayPath );
        }
        return null;
    }

    @Override
    public int getFuelLevel( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( "fuelLevel" ) ? nbt.getInteger( "fuelLevel" ) : 0;
    }
}
