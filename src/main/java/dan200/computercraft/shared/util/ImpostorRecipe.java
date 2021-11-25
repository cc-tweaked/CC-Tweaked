/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

public final class ImpostorRecipe extends ShapedRecipe
{
    private final String group;

    private ImpostorRecipe( @Nonnull ResourceLocation id, @Nonnull String group, int width, int height, NonNullList<Ingredient> ingredients, @Nonnull ItemStack result )
    {
        super( id, group, width, height, ingredients, result );
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

    public static final IRecipeSerializer<ImpostorRecipe> SERIALIZER = new BasicRecipeSerializer<ImpostorRecipe>()
    {
        @Override
        public ImpostorRecipe fromJson( @Nonnull ResourceLocation identifier, @Nonnull JsonObject json )
        {
            String group = JSONUtils.getAsString( json, "group", "" );
            ShapedRecipe recipe = IRecipeSerializer.SHAPED_RECIPE.fromJson( identifier, json );
            ItemStack result = CraftingHelper.getItemStack( JSONUtils.getAsJsonObject( json, "result" ), true );
            return new ImpostorRecipe( identifier, group, recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), result );
        }

        @Override
        public ImpostorRecipe fromNetwork( @Nonnull ResourceLocation identifier, @Nonnull PacketBuffer buf )
        {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf( Short.MAX_VALUE );
            NonNullList<Ingredient> items = NonNullList.withSize( width * height, Ingredient.EMPTY );
            for( int k = 0; k < items.size(); ++k ) items.set( k, Ingredient.fromNetwork( buf ) );
            ItemStack result = buf.readItem();
            return new ImpostorRecipe( identifier, group, width, height, items, result );
        }

        @Override
        public void toNetwork( @Nonnull PacketBuffer buf, @Nonnull ImpostorRecipe recipe )
        {
            buf.writeVarInt( recipe.getRecipeWidth() );
            buf.writeVarInt( recipe.getRecipeHeight() );
            buf.writeUtf( recipe.getGroup() );
            for( Ingredient ingredient : recipe.getIngredients() ) ingredient.toNetwork( buf );
            buf.writeItem( recipe.getResultItem() );
        }
    };
}
