/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public abstract class ComputerFamilyRecipe extends ComputerConvertRecipe {
    private final ComputerFamily family;

    public ComputerFamilyRecipe(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family) {
        super(identifier, group, category, width, height, ingredients, result);
        this.family = family;
    }

    public ComputerFamily getFamily() {
        return family;
    }

    public abstract static class Serializer<T extends ComputerFamilyRecipe> implements RecipeSerializer<T> {
        protected abstract T create(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family);

        @Override
        public T fromJson(ResourceLocation identifier, JsonObject json) {
            var group = GsonHelper.getAsString(json, "group", "");
            var category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            var family = RecipeUtil.getFamily(json, "family");

            var template = RecipeUtil.getTemplate(json);
            var result = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            return create(identifier, group, category, template.width(), template.height(), template.ingredients(), result, family);
        }

        @Override
        public T fromNetwork(ResourceLocation identifier, FriendlyByteBuf buf) {
            var width = buf.readVarInt();
            var height = buf.readVarInt();
            var group = buf.readUtf();
            var category = buf.readEnum(CraftingBookCategory.class);

            var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (var i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromNetwork(buf));

            var result = buf.readItem();
            var family = buf.readEnum(ComputerFamily.class);
            return create(identifier, group, category, width, height, ingredients, result, family);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, T recipe) {
            buf.writeVarInt(recipe.getWidth());
            buf.writeVarInt(recipe.getHeight());
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            for (var ingredient : recipe.getIngredients()) ingredient.toNetwork(buf);
            buf.writeItem(recipe.getResultItem());
            buf.writeEnum(recipe.getFamily());
        }
    }
}
