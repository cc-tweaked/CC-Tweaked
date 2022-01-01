/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

public final class ImpostorShapelessRecipe extends ShapelessRecipe
{
    private final String group;

    private ImpostorShapelessRecipe( @Nonnull ResourceLocation id, @Nonnull String group, @Nonnull ItemStack result, NonNullList<Ingredient> ingredients )
    {
        super( id, group, result, ingredients );
        this.group = group;
    }

    @Nonnull
    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inv, @Nonnull World world )
    {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingInventory inventory )
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final IRecipeSerializer<ImpostorShapelessRecipe> SERIALIZER = new BasicRecipeSerializer<ImpostorShapelessRecipe>()
    {
        @Override
        public ImpostorShapelessRecipe fromJson( @Nonnull ResourceLocation id, @Nonnull JsonObject json )
        {
            String s = JSONUtils.getAsString( json, "group", "" );
            NonNullList<Ingredient> ingredients = readIngredients( JSONUtils.getAsJsonArray( json, "ingredients" ) );

            if( ingredients.isEmpty() ) throw new JsonParseException( "No ingredients for shapeless recipe" );
            if( ingredients.size() > 9 )
            {
                throw new JsonParseException( "Too many ingredients for shapeless recipe the max is 9" );
            }

            ItemStack itemstack = CraftingHelper.getItemStack( JSONUtils.getAsJsonObject( json, "result" ), true );
            return new ImpostorShapelessRecipe( id, s, itemstack, ingredients );
        }

        private NonNullList<Ingredient> readIngredients( JsonArray arrays )
        {
            NonNullList<Ingredient> items = NonNullList.create();
            for( int i = 0; i < arrays.size(); ++i )
            {
                Ingredient ingredient = Ingredient.fromJson( arrays.get( i ) );
                if( !ingredient.isEmpty() ) items.add( ingredient );
            }

            return items;
        }

        @Override
        public ImpostorShapelessRecipe fromNetwork( @Nonnull ResourceLocation id, PacketBuffer buffer )
        {
            String s = buffer.readUtf( 32767 );
            int i = buffer.readVarInt();
            NonNullList<Ingredient> items = NonNullList.withSize( i, Ingredient.EMPTY );

            for( int j = 0; j < items.size(); j++ ) items.set( j, Ingredient.fromNetwork( buffer ) );
            ItemStack result = buffer.readItem();

            return new ImpostorShapelessRecipe( id, s, result, items );
        }

        @Override
        public void toNetwork( @Nonnull PacketBuffer buffer, @Nonnull ImpostorShapelessRecipe recipe )
        {
            buffer.writeUtf( recipe.getGroup() );
            buffer.writeVarInt( recipe.getIngredients().size() );

            for( Ingredient ingredient : recipe.getIngredients() ) ingredient.toNetwork( buffer );
            buffer.writeItem( recipe.getResultItem() );
        }
    };
}
