/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public final class ImpostorShapelessRecipe extends ShapelessRecipe {
    public static final RecipeSerializer<ImpostorShapelessRecipe> SERIALIZER = new RecipeSerializer<ImpostorShapelessRecipe>() {
        @Override
        public ImpostorShapelessRecipe read(@Nonnull Identifier id, @Nonnull JsonObject json) {
            String s = JsonHelper.getString(json, "group", "");
            DefaultedList<Ingredient> ingredients = this.readIngredients(JsonHelper.getArray(json, "ingredients"));

            if (ingredients.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            }
            if (ingredients.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
            }

            ItemStack itemstack = ShapedRecipe.getItemStack(JsonHelper.getObject(json, "result"));
            return new ImpostorShapelessRecipe(id, s, itemstack, ingredients);
        }

        private DefaultedList<Ingredient> readIngredients(JsonArray arrays) {
            DefaultedList<Ingredient> items = DefaultedList.of();
            for (int i = 0; i < arrays.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(arrays.get(i));
                if (!ingredient.isEmpty()) {
                    items.add(ingredient);
                }
            }

            return items;
        }

        @Override
        public ImpostorShapelessRecipe read(@Nonnull Identifier id, PacketByteBuf buffer) {
            String s = buffer.readString(32767);
            int i = buffer.readVarInt();
            DefaultedList<Ingredient> items = DefaultedList.ofSize(i, Ingredient.EMPTY);

            for (int j = 0; j < items.size(); j++) {
                items.set(j, Ingredient.fromPacket(buffer));
            }
            ItemStack result = buffer.readItemStack();

            return new ImpostorShapelessRecipe(id, s, result, items);
        }

        @Override
        public void write(@Nonnull PacketByteBuf buffer, @Nonnull ImpostorShapelessRecipe recipe) {
            buffer.writeString(recipe.getGroup());
            buffer.writeVarInt(recipe.getPreviewInputs()
                                     .size());

            for (Ingredient ingredient : recipe.getPreviewInputs()) {
                ingredient.write(buffer);
            }
            buffer.writeItemStack(recipe.getOutput());
        }
    };
    private final String group;

    private ImpostorShapelessRecipe(@Nonnull Identifier id, @Nonnull String group, @Nonnull ItemStack result, DefaultedList<Ingredient> ingredients) {
        super(id, group, result, ingredients);
        this.group = group;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack craft(CraftingInventory inventory) {
        return ItemStack.EMPTY;
    }
}
