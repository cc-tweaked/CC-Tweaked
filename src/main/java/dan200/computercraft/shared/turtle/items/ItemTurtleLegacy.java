/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.items;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class ItemTurtleLegacy extends ItemTurtleBase
{
    public ItemTurtleLegacy( Block block )
    {
        super( block );
        setTranslationKey( "computercraft:turtle" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
    }

    // IComputerItem implementation

    @Override
    public int getComputerID( @Nonnull ItemStack stack )
    {
        if( stack.hasTagCompound() && stack.getTagCompound().hasKey( "computerID" ) )
        {
            return stack.getTagCompound().getInteger( "computerID" );
        }
        else
        {
            int damage = stack.getItemDamage();
            return ((damage & 0xfffc) >> 2) - 1;
        }
    }

    @Override
    public ComputerFamily getFamily()
    {
        return ComputerFamily.Normal;
    }

    // ITurtleItem implementation

    @Override
    public ITurtleUpgrade getUpgrade( @Nonnull ItemStack stack, TurtleSide side )
    {
        int damage = stack.getItemDamage();
        switch( side )
        {
            case Left:
            {
                if( (damage & 0x1) > 0 )
                {
                    return ComputerCraft.TurtleUpgrades.diamondPickaxe;
                }
                break;
            }
            case Right:
            {
                if( (damage & 0x2) > 0 )
                {
                    return ComputerCraft.TurtleUpgrades.wirelessModem;
                }
                break;
            }
        }
        return null;
    }

    @Override
    public int getColour( @Nonnull ItemStack stack )
    {
        return -1;
    }

    @Override
    public ResourceLocation getOverlay( @Nonnull ItemStack stack ) { return null; }

    @Override
    public int getFuelLevel( @Nonnull ItemStack stack )
    {
        if( stack.hasTagCompound() )
        {
            NBTTagCompound nbt = stack.getTagCompound();
            return nbt.getInteger( "fuelLevel" );
        }
        return 0;
    }
}

