/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.recipes;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

public class PocketComputerUpgradeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{
    public PocketComputerUpgradeRecipe()
    {
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
        return PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, null );
    }

    @Override
    public boolean matches( @Nonnull InventoryCrafting inventory, @Nonnull World world )
    {
        return !getCraftingResult( inventory ).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting inventory )
    {
        // Scan the grid for a pocket computer
        ItemStack computer = ItemStack.EMPTY;
        int computerX = -1;
        int computerY = -1;
        for (int y = 0; y < inventory.getHeight(); ++y)
        {
            for (int x = 0; x < inventory.getWidth(); ++x)
            {
                ItemStack item = inventory.getStackInRowAndColumn( x, y );
                if( !item.isEmpty() && item.getItem() instanceof ItemPocketComputer )
                {
                    computer = item;
                    computerX = x;
                    computerY = y;
                    break;
                }
            }
            if( !computer.isEmpty() )
            {
                break;
            }
        }

        if( computer.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        ItemPocketComputer itemComputer = (ItemPocketComputer)computer.getItem();
        if( itemComputer.getUpgrade( computer ) != null )
        {
            return ItemStack.EMPTY;
        }

        // Check for upgrades around the item
        IPocketUpgrade upgrade = null;
        for (int y = 0; y < inventory.getHeight(); ++y)
        {
            for (int x = 0; x < inventory.getWidth(); ++x)
            {
                ItemStack item = inventory.getStackInRowAndColumn( x, y );
                if( x == computerX && y == computerY )
                {
                    continue;
                }
                else if( x == computerX && y == computerY - 1 )
                {
                    upgrade = ComputerCraft.getPocketUpgrade( item );
                    if( upgrade == null ) return ItemStack.EMPTY;
                }
                else if( !item.isEmpty() )
                {
                    return ItemStack.EMPTY;
                }
            }
        }

        if( upgrade == null )
        {
            return ItemStack.EMPTY;
        }

        // Construct the new stack
        ComputerFamily family = itemComputer.getFamily( computer );
        int computerID = itemComputer.getComputerID( computer );
        String label = itemComputer.getLabel( computer );
        int colour = itemComputer.getColour( computer );
        return PocketComputerItemFactory.create( computerID, label, colour, family, upgrade );
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
