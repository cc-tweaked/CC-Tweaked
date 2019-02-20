/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.ComputerCraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ImpostorShapelessRecipe extends ShapelessRecipe
{
    public ImpostorShapelessRecipe( @Nonnull ResourceLocation id, @Nonnull String group, @Nonnull ItemStack result, NonNullList<Ingredient> ingredients )
    {
        super( id, group, result, ingredients );
    }

    public ImpostorShapelessRecipe( @Nonnull ResourceLocation id, @Nonnull String group, @Nonnull ItemStack result, ItemStack[] ingredients )
    {
        super( id, group, result, convert( ingredients ) );
    }

    private static NonNullList<Ingredient> convert( ItemStack[] items )
    {
        NonNullList<Ingredient> ingredients = NonNullList.withSize( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.fromStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( IInventory inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( IInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "impostor_shapeless" );

    public static final IRecipeSerializer<ImpostorShapelessRecipe> SERIALIZER = new IRecipeSerializer<ImpostorShapelessRecipe>()
    {
        @Override
        public ImpostorShapelessRecipe read( @Nonnull ResourceLocation id, @Nonnull JsonObject json )
        {
            String s = JsonUtils.getString( json, "group", "" );
            NonNullList<Ingredient> ingredients = readIngredients( JsonUtils.getJsonArray( json, "ingredients" ) );

            if( ingredients.isEmpty() ) throw new JsonParseException( "No ingredients for shapeless recipe" );
            if( ingredients.size() > 9 )
            {
                throw new JsonParseException( "Too many ingredients for shapeless recipe the max is 9" );
            }

            ItemStack itemstack = ShapedRecipe.deserializeItem( JsonUtils.getJsonObject( json, "result" ) );
            return new ImpostorShapelessRecipe( id, s, itemstack, ingredients );
        }

        private NonNullList<Ingredient> readIngredients( JsonArray arrays )
        {
            NonNullList<Ingredient> items = NonNullList.create();
            for( int i = 0; i < arrays.size(); ++i )
            {
                Ingredient ingredient = Ingredient.deserialize( arrays.get( i ) );
                if( !ingredient.hasNoMatchingItems() ) items.add( ingredient );
            }

            return items;
        }

        @Override
        public ImpostorShapelessRecipe read( @Nonnull ResourceLocation id, PacketBuffer buffer )
        {
            String s = buffer.readString( 32767 );
            int i = buffer.readVarInt();
            NonNullList<Ingredient> items = NonNullList.<Ingredient>withSize( i, Ingredient.EMPTY );

            for( int j = 0; j < items.size(); j++ ) items.set( j, Ingredient.read( buffer ) );
            ItemStack result = buffer.readItemStack();

            return new ImpostorShapelessRecipe( id, s, result, items );
        }

        @Override
        public void write( @Nonnull PacketBuffer buffer, @Nonnull ImpostorShapelessRecipe recipe )
        {
            RecipeSerializers.CRAFTING_SHAPELESS.write( buffer, recipe );
        }

        @Nonnull
        @Override
        public ResourceLocation getName()
        {
            return ID;
        }
    };
}
