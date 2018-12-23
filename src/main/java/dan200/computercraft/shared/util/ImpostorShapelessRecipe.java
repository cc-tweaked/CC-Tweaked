/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class ImpostorShapelessRecipe extends ShapelessRecipes
{
    public ImpostorShapelessRecipe( @Nonnull String group, @Nonnull ItemStack result, NonNullList<Ingredient> ingredients )
    {
        super( group, result, ingredients );
    }

    public ImpostorShapelessRecipe( @Nonnull String group, @Nonnull ItemStack result, ItemStack[] ingredients )
    {
        super( group, result, convert( ingredients ) );
    }

    private static NonNullList<Ingredient> convert( ItemStack[] items )
    {
        NonNullList<Ingredient> ingredients = NonNullList.withSize( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.fromStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( InventoryCrafting inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( InventoryCrafting inventory )
    {
        return ItemStack.EMPTY;
    }

    public static class Factory implements IRecipeFactory
    {
        @Override
        public IRecipe parse( JsonContext context, JsonObject json )
        {
            String group = JsonUtils.getString( json, "group", "" );
            NonNullList<Ingredient> ings = RecipeUtil.getIngredients( context, json );
            ItemStack itemstack = ShapedRecipes.deserializeItem( JsonUtils.getJsonObject( json, "result" ), true );
            return new ImpostorShapelessRecipe( group, itemstack, ings );
        }
    }
}
