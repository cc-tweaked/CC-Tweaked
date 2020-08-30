/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import java.util.function.Consumer;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
public final class RecipeWrapper implements RecipeJsonProvider
{
    private final RecipeJsonProvider recipe;
    private final CompoundTag resultData;
    private final RecipeSerializer<?> serializer;

    private RecipeWrapper( RecipeJsonProvider recipe, CompoundTag resultData, RecipeSerializer<?> serializer )
    {
        this.resultData = resultData;
        this.recipe = recipe;
        this.serializer = serializer;
    }

    public static Consumer<RecipeJsonProvider> wrap( RecipeSerializer<?> serializer, Consumer<RecipeJsonProvider> original )
    {
        return x -> original.accept( new RecipeWrapper( x, null, serializer ) );
    }

    public static Consumer<RecipeJsonProvider> wrap( RecipeSerializer<?> serializer, Consumer<RecipeJsonProvider> original, CompoundTag resultData )
    {
        return x -> original.accept( new RecipeWrapper( x, resultData, serializer ) );
    }

    public static Consumer<RecipeJsonProvider> wrap( RecipeSerializer<?> serializer, Consumer<RecipeJsonProvider> original, Consumer<CompoundTag> resultData )
    {
        CompoundTag tag = new CompoundTag();
        resultData.accept( tag );
        return x -> original.accept( new RecipeWrapper( x, tag, serializer ) );
    }

    @Override
    public void serialize( @Nonnull JsonObject jsonObject )
    {
        recipe.serialize( jsonObject );

        if( resultData != null )
        {
            JsonObject object = JsonHelper.getObject( jsonObject, "result" );
            object.addProperty( "nbt", resultData.toString() );
        }
    }

    @Nonnull
    @Override
    public Identifier getRecipeId()
    {
        return recipe.getRecipeId();
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return serializer;
    }

    @Nullable
    @Override
    public JsonObject toAdvancementJson()
    {
        return recipe.toAdvancementJson();
    }

    @Nullable
    @Override
    public Identifier getAdvancementId()
    {
        return recipe.getAdvancementId();
    }
}
