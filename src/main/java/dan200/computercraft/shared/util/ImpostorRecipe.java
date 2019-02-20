/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import dan200.computercraft.ComputerCraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeSerializers;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ImpostorRecipe extends ShapedRecipe
{
    public ImpostorRecipe( @Nonnull ResourceLocation id, @Nonnull String group, int width, int height, NonNullList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, ingredients, result );
    }

    public ImpostorRecipe( @Nonnull ResourceLocation id, @Nonnull String group, int width, int height, ItemStack[] ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, convert( ingredients ), result );
    }

    private static NonNullList<Ingredient> convert( ItemStack[] items )
    {
        NonNullList<Ingredient> ingredients = NonNullList.withSize( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.fromStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( @Nonnull IInventory inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull IInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return serializer;
    }

    private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, ":imposter_recipe" );
    private static final IRecipeSerializer<ImpostorRecipe> serializer = new IRecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe read( @Nonnull ResourceLocation identifier, @Nonnull JsonObject json )
        {
            // TODO: This will probably explode on servers
            ShapedRecipe shaped = RecipeSerializers.CRAFTING_SHAPED.read( identifier, json );
            return new ImpostorRecipe( shaped.getId(), shaped.getGroup(), shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(), shaped.getRecipeOutput() );
        }

        @Override
        public ImpostorRecipe read( @Nonnull ResourceLocation identifier, @Nonnull PacketBuffer buf )
        {
            ShapedRecipe shaped = RecipeSerializers.CRAFTING_SHAPED.read( identifier, buf );
            return new ImpostorRecipe( shaped.getId(), shaped.getGroup(), shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(), shaped.getRecipeOutput() );
        }

        @Override
        public void write( @Nonnull PacketBuffer packet, @Nonnull ImpostorRecipe recipe )
        {
            RecipeSerializers.CRAFTING_SHAPED.write( packet, recipe );
        }

        @Override
        public ResourceLocation getName()
        {
            return ID;
        }
    };
}
