/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

public class DiskRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{
    private final Ingredient paper = new OreIngredient( "paper" );
    private final Ingredient redstone = new OreIngredient( "dustRedstone" );
    
    @Override
    public boolean matches( @Nonnull InventoryCrafting inv, @Nonnull World world )
    {
        boolean paperFound = false;
        boolean redstoneFound = false;

        for( int i = 0; i < inv.getSizeInventory(); ++i )
        {
            ItemStack stack = inv.getStackInSlot( i );

            if( !stack.isEmpty() )
            {
                if( paper.apply( stack ) )
                {
                    if( paperFound ) return false;
                    paperFound = true;
                }
                else if( redstone.apply( stack ) )
                {
                    if( redstoneFound ) return false;
                    redstoneFound = true;
                }
                else if( ColourUtils.getStackColour( stack ) < 0 )
                {
                    return false;
                }
            }
        }

        return redstoneFound && paperFound;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting inv )
    {
        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.getSizeInventory(); ++i )
        {
            ItemStack stack = inv.getStackInSlot( i );

            if( stack.isEmpty() ) continue;

            if( !paper.apply( stack ) && !redstone.apply( stack ) )
            {
                int index = ColourUtils.getStackColour( stack );
                if( index < 0 ) continue;

                Colour colour = Colour.values()[ index ];
                tracker.addColour( colour.getR(), colour.getG(), colour.getB() );
            }
        }

        return ItemDisk.createFromIDAndColour( -1, null, tracker.hasColour() ? tracker.getColour() : Colour.Blue.getHex() );
    }

    @Override
    public boolean canFit( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput()
    {
        return ItemDisk.createFromIDAndColour( -1, null, Colour.Blue.getHex() );
    }

    @Nonnull
    @Override
    public NonNullList<ItemStack> getRemainingItems( @Nonnull InventoryCrafting inventoryCrafting )
    {
        NonNullList<ItemStack> results = NonNullList.withSize( inventoryCrafting.getSizeInventory(), ItemStack.EMPTY );
        for( int i = 0; i < results.size(); ++i )
        {
            ItemStack stack = inventoryCrafting.getStackInSlot( i );
            results.set( i, ForgeHooks.getContainerItem( stack ) );
        }
        return results;
    }
}
