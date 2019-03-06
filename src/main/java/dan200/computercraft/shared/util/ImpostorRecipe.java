/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.crafting.ShapedRecipe;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ImpostorRecipe extends ShapedRecipe
{
    public ImpostorRecipe( @Nonnull Identifier id, @Nonnull String group, int width, int height, DefaultedList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, ingredients, result );
    }

    public ImpostorRecipe( @Nonnull Identifier id, @Nonnull String group, int width, int height, ItemStack[] ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, convert( ingredients ), result );
    }

    private static DefaultedList<Ingredient> convert( ItemStack[] items )
    {
        DefaultedList<Ingredient> ingredients = DefaultedList.create( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.ofStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack craft( @Nonnull CraftingInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final RecipeSerializer<ImpostorRecipe> SERIALIZER = new RecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe read( Identifier identifier, JsonObject json )
        {
            // TODO: This will explode on servers.
            ShapedRecipe shaped = RecipeSerializer.SHAPED.read( identifier, json );
            return new ImpostorRecipe( shaped.getId(), shaped.getGroup(), shaped.getWidth(), shaped.getHeight(), shaped.getPreviewInputs(), shaped.getOutput() );
        }

        @Override
        public ImpostorRecipe read( Identifier identifier, PacketByteBuf buf )
        {
            ShapedRecipe shaped = RecipeSerializer.SHAPED.read( identifier, buf );
            return new ImpostorRecipe( shaped.getId(), shaped.getGroup(), shaped.getWidth(), shaped.getHeight(), shaped.getPreviewInputs(), shaped.getOutput() );
        }

        @Override
        public void write( PacketByteBuf packet, ImpostorRecipe recipe )
        {
            RecipeSerializer.SHAPED.write( packet, recipe );
        }
    };
}
