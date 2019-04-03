/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.recipes;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.crafting.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public final class PocketComputerUpgradeRecipe extends SpecialCraftingRecipe
{
    private PocketComputerUpgradeRecipe( Identifier identifier )
    {
        super( identifier );
    }

    @Override
    public boolean fits( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Nonnull
    @Override
    public ItemStack getOutput()
    {
        return PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, null );
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inventory, @Nonnull World world )
    {
        return !craft( inventory ).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack craft( @Nonnull CraftingInventory inventory )
    {
        // Scan the grid for a pocket computer
        ItemStack computer = ItemStack.EMPTY;
        int computerX = -1;
        int computerY = -1;
        computer:
        for( int y = 0; y < inventory.getHeight(); y++ )
        {
            for( int x = 0; x < inventory.getWidth(); x++ )
            {
                ItemStack item = inventory.getInvStack( x + y * inventory.getWidth() );
                if( !item.isEmpty() && item.getItem() instanceof ItemPocketComputer )
                {
                    computer = item;
                    computerX = x;
                    computerY = y;
                    break computer;
                }
            }
        }

        if( computer.isEmpty() ) return ItemStack.EMPTY;

        ItemPocketComputer itemComputer = (ItemPocketComputer) computer.getItem();
        if( ItemPocketComputer.getUpgrade( computer ) != null ) return ItemStack.EMPTY;

        // Check for upgrades around the item
        IPocketUpgrade upgrade = null;
        for( int y = 0; y < inventory.getHeight(); y++ )
        {
            for( int x = 0; x < inventory.getWidth(); x++ )
            {
                ItemStack item = inventory.getInvStack( x + y * inventory.getWidth() );
                if( x == computerX && y == computerY ) continue;

                if( x == computerX && y == computerY - 1 )
                {
                    upgrade = PocketUpgrades.get( item );
                    if( upgrade == null ) return ItemStack.EMPTY;
                }
                else if( !item.isEmpty() )
                {
                    return ItemStack.EMPTY;
                }
            }
        }

        if( upgrade == null ) return ItemStack.EMPTY;

        // Construct the new stack
        ComputerFamily family = itemComputer.getFamily();
        int computerID = itemComputer.getComputerID( computer );
        String label = itemComputer.getLabel( computer );
        int colour = itemComputer.getColour( computer );
        return PocketComputerItemFactory.create( computerID, label, colour, family, upgrade );
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final RecipeSerializer<PocketComputerUpgradeRecipe> SERIALIZER = new SpecialRecipeSerializer<>( PocketComputerUpgradeRecipe::new );
}
