/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
final class RecipeWrapper implements Consumer<IFinishedRecipe>
{
    private final Consumer<IFinishedRecipe> add;
    private final IRecipeSerializer<?> serializer;
    private final List<Consumer<JsonObject>> extend = new ArrayList<>( 0 );

    RecipeWrapper( Consumer<IFinishedRecipe> add, IRecipeSerializer<?> serializer )
    {
        this.add = add;
        this.serializer = serializer;
    }

    public static RecipeWrapper wrap( IRecipeSerializer<?> serializer, Consumer<IFinishedRecipe> original )
    {
        return new RecipeWrapper( original, serializer );
    }

    public RecipeWrapper withExtraData( Consumer<JsonObject> extra )
    {
        extend.add( extra );
        return this;
    }

    public RecipeWrapper withResultTag( @Nullable CompoundNBT resultTag )
    {
        if( resultTag == null ) return this;

        extend.add( json -> {
            JsonObject object = JSONUtils.getAsJsonObject( json, "result" );
            object.addProperty( "nbt", resultTag.toString() );
        } );
        return this;
    }

    public RecipeWrapper withResultTag( Consumer<CompoundNBT> resultTag )
    {
        CompoundNBT tag = new CompoundNBT();
        resultTag.accept( tag );
        return withResultTag( tag );
    }

    @Override
    public void accept( IFinishedRecipe finishedRecipe )
    {
        add.accept( new RecipeImpl( finishedRecipe, serializer, extend ) );
    }

    private static final class RecipeImpl implements IFinishedRecipe
    {
        private final IFinishedRecipe recipe;
        private final IRecipeSerializer<?> serializer;
        private final List<Consumer<JsonObject>> extend;

        private RecipeImpl( IFinishedRecipe recipe, IRecipeSerializer<?> serializer, List<Consumer<JsonObject>> extend )
        {
            this.recipe = recipe;
            this.serializer = serializer;
            this.extend = extend;
        }

        @Override
        public void serializeRecipeData( @Nonnull JsonObject jsonObject )
        {
            recipe.serializeRecipeData( jsonObject );
            for( Consumer<JsonObject> extender : extend ) extender.accept( jsonObject );
        }

        @Nonnull
        @Override
        public ResourceLocation getId()
        {
            return recipe.getId();
        }

        @Nonnull
        @Override
        public IRecipeSerializer<?> getType()
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
}
