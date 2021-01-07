/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
public final class RecipeWrapper implements IFinishedRecipe
{
    private final IFinishedRecipe recipe;
    private final CompoundNBT resultData;
    private final IRecipeSerializer<?> serializer;

    private RecipeWrapper( IFinishedRecipe recipe, CompoundNBT resultData, IRecipeSerializer<?> serializer )
    {
        this.resultData = resultData;
        this.recipe = recipe;
        this.serializer = serializer;
    }

    public static Consumer<IFinishedRecipe> wrap( IRecipeSerializer<?> serializer, Consumer<IFinishedRecipe> original )
    {
        return x -> original.accept( new RecipeWrapper( x, null, serializer ) );
    }

    public static Consumer<IFinishedRecipe> wrap( IRecipeSerializer<?> serializer, Consumer<IFinishedRecipe> original, CompoundNBT resultData )
    {
        return x -> original.accept( new RecipeWrapper( x, resultData, serializer ) );
    }

    public static Consumer<IFinishedRecipe> wrap( IRecipeSerializer<?> serializer, Consumer<IFinishedRecipe> original, Consumer<CompoundNBT> resultData )
    {
        CompoundNBT tag = new CompoundNBT();
        resultData.accept( tag );
        return x -> original.accept( new RecipeWrapper( x, tag, serializer ) );
    }

    @Override
    public void serialize( @Nonnull JsonObject jsonObject )
    {
        recipe.serialize( jsonObject );

        if( resultData != null )
        {
            JsonObject object = JSONUtils.getJsonObject( jsonObject, "result" );
            object.addProperty( "nbt", resultData.toString() );
        }
    }

    @Nonnull
    @Override
    public ResourceLocation getID()
    {
        return recipe.getID();
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return serializer;
    }

    @Nullable
    @Override
    public JsonObject getAdvancementJson()
    {
        return recipe.getAdvancementJson();
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementID()
    {
        return recipe.getAdvancementID();
    }
}
