// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.recipes;

import com.google.gson.JsonObject;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.CustomShapelessRecipe;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A {@link ShapelessRecipe} which sets the {@linkplain TurtleItem#getOverlay(ItemStack)} turtle's overlay} instead.
 */
public class TurtleOverlayRecipe extends CustomShapelessRecipe {
    private final ResourceLocation overlay;

    public TurtleOverlayRecipe(ResourceLocation id, ShapelessRecipeSpec spec, ResourceLocation overlay) {
        super(id, spec);
        this.overlay = overlay;
    }

    private static ItemStack make(ItemStack stack, ResourceLocation overlay) {
        var turtle = (TurtleItem) stack.getItem();
        return turtle.create(
            turtle.getComputerID(stack),
            turtle.getLabel(stack),
            turtle.getColour(stack),
            turtle.getUpgradeWithData(stack, TurtleSide.LEFT),
            turtle.getUpgradeWithData(stack, TurtleSide.RIGHT),
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
    public RecipeSerializer<TurtleOverlayRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE_OVERLAY.get();
    }

    public static class Serialiser implements RecipeSerializer<TurtleOverlayRecipe> {
        @Override
        public TurtleOverlayRecipe fromJson(ResourceLocation id, JsonObject json) {
            var recipe = ShapelessRecipeSpec.fromJson(json);
            var overlay = new ResourceLocation(GsonHelper.getAsString(json, "overlay"));

            return new TurtleOverlayRecipe(id, recipe, overlay);
        }

        @Override
        public TurtleOverlayRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            var recipe = ShapelessRecipeSpec.fromNetwork(buffer);
            var overlay = buffer.readResourceLocation();
            return new TurtleOverlayRecipe(id, recipe, overlay);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TurtleOverlayRecipe recipe) {
            recipe.toSpec().toNetwork(buffer);
            buffer.writeResourceLocation(recipe.overlay);
        }
    }
}
