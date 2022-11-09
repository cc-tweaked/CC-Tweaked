/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class ImpostorShapelessRecipe extends ShapelessRecipe {
    private final String group;

    private ImpostorShapelessRecipe(@Nonnull ResourceLocation id, @Nonnull String group, @Nonnull ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, result, ingredients);
        this.group = group;
    }

    @Nonnull
    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level world) {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inventory) {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get();
    }

    public static final class Serializer implements RecipeSerializer<ImpostorShapelessRecipe> {
        @Nonnull
        @Override
        public ImpostorShapelessRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            var s = GsonHelper.getAsString(json, "group", "");
            var ingredients = readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (ingredients.isEmpty()) throw new JsonParseException("No ingredients for shapeless recipe");
            if (ingredients.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
            }

            var result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ImpostorShapelessRecipe(id, s, result, ingredients);
        }

        private NonNullList<Ingredient> readIngredients(JsonArray arrays) {
            NonNullList<Ingredient> items = NonNullList.create();
            for (var i = 0; i < arrays.size(); ++i) {
                var ingredient = Ingredient.fromJson(arrays.get(i));
                if (!ingredient.isEmpty()) items.add(ingredient);
            }

            return items;
        }

        @Override
        public ImpostorShapelessRecipe fromNetwork(@Nonnull ResourceLocation id, FriendlyByteBuf buffer) {
            var s = buffer.readUtf(32767);
            var i = buffer.readVarInt();
            var items = NonNullList.withSize(i, Ingredient.EMPTY);

            for (var j = 0; j < items.size(); j++) items.set(j, Ingredient.fromNetwork(buffer));
            var result = buffer.readItem();

            return new ImpostorShapelessRecipe(id, s, result, items);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull ImpostorShapelessRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeVarInt(recipe.getIngredients().size());

            for (var ingredient : recipe.getIngredients()) ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.getResultItem());
        }
    }
}
