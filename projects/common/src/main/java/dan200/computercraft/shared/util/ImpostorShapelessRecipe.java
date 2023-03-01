// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class ImpostorShapelessRecipe extends ShapelessRecipe {
    private final String group;

    private ImpostorShapelessRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
        this.group = group;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get();
    }

    public static final class Serializer implements RecipeSerializer<ImpostorShapelessRecipe> {
        @Override
        public ImpostorShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            var group = GsonHelper.getAsString(json, "group", "");
            var category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            var ingredients = readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (ingredients.isEmpty()) throw new JsonParseException("No ingredients for shapeless recipe");
            if (ingredients.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
            }

            var result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ImpostorShapelessRecipe(id, group, category, result, ingredients);
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
        public ImpostorShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            var group = buffer.readUtf();
            var category = buffer.readEnum(CraftingBookCategory.class);
            var count = buffer.readVarInt();
            var items = NonNullList.withSize(count, Ingredient.EMPTY);

            for (var j = 0; j < items.size(); j++) items.set(j, Ingredient.fromNetwork(buffer));
            var result = buffer.readItem();

            return new ImpostorShapelessRecipe(id, group, category, result, items);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ImpostorShapelessRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.getIngredients().size());

            for (var ingredient : recipe.getIngredients()) ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.getResultItem());
        }
    }
}
