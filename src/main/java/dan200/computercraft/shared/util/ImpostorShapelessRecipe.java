/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.crafting.ShapedRecipe;
import net.minecraft.recipe.crafting.ShapelessRecipe;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ImpostorShapelessRecipe extends ShapelessRecipe
{
    public ImpostorShapelessRecipe( @Nonnull Identifier id, @Nonnull String group, @Nonnull ItemStack result, DefaultedList<Ingredient> ingredients )
    {
        super( id, group, result, ingredients );
    }

    public ImpostorShapelessRecipe( @Nonnull Identifier id, @Nonnull String group, @Nonnull ItemStack result, ItemStack[] ingredients )
    {
        super( id, group, result, convert( ingredients ) );
    }

    private static DefaultedList<Ingredient> convert( ItemStack[] items )
    {
        DefaultedList<Ingredient> ingredients = DefaultedList.create( items.length, Ingredient.EMPTY );
        for( int i = 0; i < items.length; i++ ) ingredients.set( i, Ingredient.ofStacks( items[i] ) );
        return ingredients;
    }

    @Override
    public boolean matches( CraftingInventory inv, World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack craft( CraftingInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final RecipeSerializer<ImpostorShapelessRecipe> SERIALIZER = new RecipeSerializer<ImpostorShapelessRecipe>()
    {
        @Override
        public ImpostorShapelessRecipe read( @Nonnull Identifier id, @Nonnull JsonObject json )
        {
            String s = JsonHelper.getString( json, "group", "" );
            DefaultedList<Ingredient> ingredients = readIngredients( JsonHelper.getArray( json, "ingredients" ) );

            if( ingredients.isEmpty() ) throw new JsonParseException( "No ingredients for shapeless recipe" );
            if( ingredients.size() > 9 )
            {
                throw new JsonParseException( "Too many ingredients for shapeless recipe the max is 9" );
            }

            ItemStack itemstack = ShapedRecipe.deserializeItemStack( JsonHelper.getObject( json, "result" ) );
            return new ImpostorShapelessRecipe( id, s, itemstack, ingredients );
        }

        private DefaultedList<Ingredient> readIngredients( JsonArray arrays )
        {
            DefaultedList<Ingredient> items = DefaultedList.create();
            for( int i = 0; i < arrays.size(); ++i )
            {
                Ingredient ingredient = Ingredient.fromJson( arrays.get( i ) );
                if( !ingredient.isEmpty() ) items.add( ingredient );
            }

            return items;
        }

        @Override
        public ImpostorShapelessRecipe read( @Nonnull Identifier id, PacketByteBuf buffer )
        {
            String s = buffer.readString( 32767 );
            int i = buffer.readVarInt();
            DefaultedList<Ingredient> items = DefaultedList.create( i, Ingredient.EMPTY );

            for( int j = 0; j < items.size(); j++ ) items.set( j, Ingredient.fromPacket( buffer ) );
            ItemStack result = buffer.readItemStack();

            return new ImpostorShapelessRecipe( id, s, result, items );
        }

        @Override
        public void write( @Nonnull PacketByteBuf buffer, @Nonnull ImpostorShapelessRecipe recipe )
        {
            RecipeSerializer.SHAPELESS.write( buffer, recipe );
        }
    };
}
