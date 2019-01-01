/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class ImpostorRecipe extends ShapedRecipes
{
    public ImpostorRecipe( @Nonnull String group, int width, int height, NonNullList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( group, width, height, ingredients, result );
    }

    public ImpostorRecipe( @Nonnull String group, int width, int height, ItemStack[] ingredients, @Nonnull ItemStack result )
    {
        super( group, width, height, convert( ingredients ), result );
    }

    private static NonNullList<Ingredient> convert( ItemStack[] items )
    {
        NonNullList<Ingredient> ingredients = NonNullList.withSize( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.fromStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( @Nonnull InventoryCrafting inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting _inventory )
    {
        return ItemStack.EMPTY;
    }

    public static class Factory implements IRecipeFactory
    {
        @Override
        public IRecipe parse( JsonContext ctx, JsonObject json )
        {
            String group = JsonUtils.getString( json, "group", "" );
            CraftingHelper.ShapedPrimer primer = RecipeUtil.getPrimer( ctx, json );
            ItemStack result = CraftingHelper.getItemStack( JsonUtils.getJsonObject( json, "result" ), ctx );
            return new ImpostorRecipe( group, primer.width, primer.height, primer.input, result );
        }
    }
}
