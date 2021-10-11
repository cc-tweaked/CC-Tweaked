/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public final class ImpostorRecipe extends ShapedRecipe
{
    public static final RecipeSerializer<ImpostorRecipe> SERIALIZER = new RecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe read( @Nonnull Identifier identifier, @Nonnull JsonObject json )
        {
            String group = JsonHelper.getString( json, "group", "" );
            ShapedRecipe recipe = RecipeSerializer.SHAPED.read( identifier, json );
            JsonObject resultObject = JsonHelper.getObject( json, "result" );
            ItemStack itemStack = ShapedRecipe.outputFromJson( resultObject );
            RecipeUtil.setNbt( itemStack, resultObject );
            return new ImpostorRecipe( identifier, group, recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), itemStack );
        }

        @Override
        public ImpostorRecipe read( @Nonnull Identifier identifier, @Nonnull PacketByteBuf buf )
        {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readString( Short.MAX_VALUE );
            DefaultedList<Ingredient> items = DefaultedList.ofSize( width * height, Ingredient.EMPTY );
            for( int k = 0; k < items.size(); ++k )
            {
                items.set( k, Ingredient.fromPacket( buf ) );
            }
            ItemStack result = buf.readItemStack();
            return new ImpostorRecipe( identifier, group, width, height, items, result );
        }

        @Override
        public void write( @Nonnull PacketByteBuf buf, @Nonnull ImpostorRecipe recipe )
        {
            buf.writeVarInt( recipe.getWidth() );
            buf.writeVarInt( recipe.getHeight() );
            buf.writeString( recipe.getGroup() );
            for( Ingredient ingredient : recipe.getIngredients() )
            {
                ingredient.write( buf );
            }
            buf.writeItemStack( recipe.getOutput() );
        }
    };
    private final String group;

    private ImpostorRecipe( @Nonnull Identifier id, @Nonnull String group, int width, int height, DefaultedList<Ingredient> ingredients,
                            @Nonnull ItemStack result )
    {
        super( id, group, width, height, ingredients, result );
        this.group = group;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
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
    public ItemStack craft( @Nonnull CraftingInventory inventory )
    {
        return ItemStack.EMPTY;
    }
}
