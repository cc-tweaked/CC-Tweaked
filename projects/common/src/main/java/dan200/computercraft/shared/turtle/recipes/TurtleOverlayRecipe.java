// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/**
 * A {@link ShapelessRecipe} which sets the {@linkplain TurtleItem#getOverlay(ItemStack)} turtle's overlay} instead.
 */
public class TurtleOverlayRecipe extends ShapelessRecipe {
    private final ResourceLocation overlay;
    private final ItemStack result;

    public TurtleOverlayRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, ResourceLocation overlay) {
        super(id, group, category, result, ingredients);
        this.overlay = overlay;
        this.result = result;
    }

    private static ItemStack make(ItemStack stack, ResourceLocation overlay) {
        var turtle = (TurtleItem) stack.getItem();
        return turtle.create(
            turtle.getComputerID(stack),
            turtle.getLabel(stack),
            turtle.getColour(stack),
            turtle.getUpgrade(stack, TurtleSide.LEFT),
            turtle.getUpgrade(stack, TurtleSide.RIGHT),
            turtle.getFuelLevel(stack),
            overlay
        );
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.getItem() instanceof TurtleItem) return make(stack, overlay);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE_OVERLAY.get();
    }

    public static class Serializer implements RecipeSerializer<TurtleOverlayRecipe> {
        @Override
        public TurtleOverlayRecipe fromJson(ResourceLocation id, JsonObject json) {
            var group = GsonHelper.getAsString(json, "group", "");
            var category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            var ingredients = readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (ingredients.isEmpty()) throw new JsonParseException("No ingredients for shapeless recipe");
            if (ingredients.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
            }

            var overlay = new ResourceLocation(GsonHelper.getAsString(json, "overlay"));

            // We could derive this from the ingredients, but we want to avoid evaluating the ingredients too early, so
            // it's easier to do this.
            var result = make(ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")), overlay);

            return new TurtleOverlayRecipe(id, group, category, result, ingredients, overlay);
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
        public TurtleOverlayRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            var group = buffer.readUtf();
            var category = buffer.readEnum(CraftingBookCategory.class);
            var count = buffer.readVarInt();
            var items = NonNullList.withSize(count, Ingredient.EMPTY);

            for (var j = 0; j < items.size(); j++) items.set(j, Ingredient.fromNetwork(buffer));
            var result = buffer.readItem();
            var overlay = buffer.readResourceLocation();

            return new TurtleOverlayRecipe(id, group, category, result, items, overlay);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TurtleOverlayRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.getIngredients().size());

            for (var ingredient : recipe.getIngredients()) ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeResourceLocation(recipe.overlay);
        }
    }
}
