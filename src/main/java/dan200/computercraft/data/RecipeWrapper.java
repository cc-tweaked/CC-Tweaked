/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
final class RecipeWrapper implements FinishedRecipe
{
    private final FinishedRecipe recipe;
    private final CompoundTag resultData;
    private final RecipeSerializer<?> serializer;

    private RecipeWrapper( FinishedRecipe recipe, CompoundTag resultData, RecipeSerializer<?> serializer )
    {
        this.resultData = resultData;
        this.recipe = recipe;
        this.serializer = serializer;
    }

    public static Consumer<FinishedRecipe> wrap( RecipeSerializer<?> serializer, Consumer<FinishedRecipe> original )
    {
        return x -> original.accept( new RecipeWrapper( x, null, serializer ) );
    }

    public static Consumer<FinishedRecipe> wrap( RecipeSerializer<?> serializer, Consumer<FinishedRecipe> original, CompoundTag resultData )
    {
        return x -> original.accept( new RecipeWrapper( x, resultData, serializer ) );
    }

    public static Consumer<FinishedRecipe> wrap( RecipeSerializer<?> serializer, Consumer<FinishedRecipe> original, Consumer<CompoundTag> resultData )
    {
        CompoundTag tag = new CompoundTag();
        resultData.accept( tag );
        return x -> original.accept( new RecipeWrapper( x, tag, serializer ) );
    }

    @Override
    public void serializeRecipeData( @Nonnull JsonObject jsonObject )
    {
        recipe.serializeRecipeData( jsonObject );

        if( resultData != null )
        {
            JsonObject object = GsonHelper.getAsJsonObject( jsonObject, "result" );
            object.addProperty( "nbt", resultData.toString() );
        }
    }

    @Nonnull
    @Override
    public ResourceLocation getId()
    {
        return recipe.getId();
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getType()
    {
        return serializer;
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement()
    {
        return recipe.serializeAdvancement();
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId()
    {
        return recipe.getAdvancementId();
    }
}
