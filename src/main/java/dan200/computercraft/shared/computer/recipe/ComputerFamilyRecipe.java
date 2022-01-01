/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.BasicRecipeSerializer;
import dan200.computercraft.shared.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nonnull;

public abstract class ComputerFamilyRecipe extends ComputerConvertRecipe
{
    private final ComputerFamily family;

    public ComputerFamilyRecipe( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
    {
        super( identifier, group, width, height, ingredients, result );
        this.family = family;
    }

    public ComputerFamily getFamily()
    {
        return family;
    }

    public abstract static class Serializer<T extends ComputerFamilyRecipe> extends BasicRecipeSerializer<T>
    {
        protected abstract T create( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family );

        @Nonnull
        @Override
        public T fromJson( @Nonnull ResourceLocation identifier, @Nonnull JsonObject json )
        {
            String group = GsonHelper.getAsString( json, "group", "" );
            ComputerFamily family = RecipeUtil.getFamily( json, "family" );

            RecipeUtil.ShapedTemplate template = RecipeUtil.getTemplate( json );
            ItemStack result = itemStackFromJson( GsonHelper.getAsJsonObject( json, "result" ) );

            return create( identifier, group, template.width, template.height, template.ingredients, result, family );
        }

        @Nonnull
        @Override
        public T fromNetwork( @Nonnull ResourceLocation identifier, @Nonnull FriendlyByteBuf buf )
        {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            String group = buf.readUtf( Short.MAX_VALUE );

            NonNullList<Ingredient> ingredients = NonNullList.withSize( width * height, Ingredient.EMPTY );
            for( int i = 0; i < ingredients.size(); i++ ) ingredients.set( i, Ingredient.fromNetwork( buf ) );

            ItemStack result = buf.readItem();
            ComputerFamily family = buf.readEnum( ComputerFamily.class );
            return create( identifier, group, width, height, ingredients, result, family );
        }

        @Override
        public void toNetwork( @Nonnull FriendlyByteBuf buf, @Nonnull T recipe )
        {
            buf.writeVarInt( recipe.getWidth() );
            buf.writeVarInt( recipe.getHeight() );
            buf.writeUtf( recipe.getGroup() );
            for( Ingredient ingredient : recipe.getIngredients() ) ingredient.toNetwork( buf );
            buf.writeItem( recipe.getResultItem() );
            buf.writeEnum( recipe.getFamily() );
        }
    }
}
