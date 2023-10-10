// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
final class RecipeWrapper implements Consumer<FinishedRecipe> {
    private final Consumer<FinishedRecipe> add;
    private final RecipeSerializer<?> serializer;
    private final List<Consumer<JsonObject>> extend = new ArrayList<>(0);

    RecipeWrapper(Consumer<FinishedRecipe> add, RecipeSerializer<?> serializer) {
        this.add = add;
        this.serializer = serializer;
    }

    public static RecipeWrapper wrap(RecipeSerializer<?> serializer, Consumer<FinishedRecipe> original) {
        return new RecipeWrapper(original, serializer);
    }

    public RecipeWrapper withExtraData(Consumer<JsonObject> extra) {
        extend.add(extra);
        return this;
    }

    public RecipeWrapper withResultTag(@Nullable CompoundTag resultTag) {
        if (resultTag == null) return this;

        extend.add(json -> {
            var object = GsonHelper.getAsJsonObject(json, "result");
            object.addProperty("nbt", resultTag.toString());
        });
        return this;
    }

    public RecipeWrapper withResultTag(Consumer<CompoundTag> resultTag) {
        var tag = new CompoundTag();
        resultTag.accept(tag);
        return withResultTag(tag);
    }

    @Override
    public void accept(FinishedRecipe finishedRecipe) {
        add.accept(new RecipeImpl(finishedRecipe, serializer, extend));
    }

    private record RecipeImpl(
        FinishedRecipe recipe, RecipeSerializer<?> serializer, List<Consumer<JsonObject>> extend
    ) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject jsonObject) {
            recipe.serializeRecipeData(jsonObject);
            for (var extender : extend) extender.accept(jsonObject);
        }

        @Override
        public ResourceLocation getId() {
            return recipe.getId();
        }

        @Override
        public RecipeSerializer<?> getType() {
            return serializer;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return recipe.serializeAdvancement();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return recipe.getAdvancementId();
        }
    }
}
