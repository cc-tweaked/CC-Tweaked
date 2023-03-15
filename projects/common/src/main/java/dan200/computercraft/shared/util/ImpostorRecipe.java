// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class ImpostorRecipe extends ShapedRecipe {
    private final String group;
    private final ItemStack result;

    private ImpostorRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(id, group, category, width, height, ingredients, result);
        this.group = group;
        this.result = result;
    }

    @Override
    public String getGroup() {
        return group;
    }

    ItemStack getResultItem() {
        return result;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.IMPOSTOR_SHAPED.get();
    }

    public static class Serializer implements RecipeSerializer<ImpostorRecipe> {
        @Override
        public ImpostorRecipe fromJson(ResourceLocation identifier, JsonObject json) {
            var group = GsonHelper.getAsString(json, "group", "");
            var category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            var recipe = RecipeSerializer.SHAPED_RECIPE.fromJson(identifier, json);
            var result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ImpostorRecipe(identifier, group, category, recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), result);
        }

        @Override
        public ImpostorRecipe fromNetwork(ResourceLocation identifier, FriendlyByteBuf buf) {
            var width = buf.readVarInt();
            var height = buf.readVarInt();
            var group = buf.readUtf(Short.MAX_VALUE);
            var category = buf.readEnum(CraftingBookCategory.class);
            var items = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (var k = 0; k < items.size(); k++) items.set(k, Ingredient.fromNetwork(buf));
            var result = buf.readItem();
            return new ImpostorRecipe(identifier, group, category, width, height, items, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ImpostorRecipe recipe) {
            buf.writeVarInt(recipe.getWidth());
            buf.writeVarInt(recipe.getHeight());
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            for (var ingredient : recipe.getIngredients()) ingredient.toNetwork(buf);
            buf.writeItem(recipe.getResultItem());
        }
    }
}
