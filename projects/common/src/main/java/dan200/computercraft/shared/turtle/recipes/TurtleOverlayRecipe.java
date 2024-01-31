// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.CustomShapelessRecipe;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A {@link ShapelessRecipe} which sets the {@linkplain TurtleItem#getOverlay(ItemStack)} turtle's overlay} instead.
 */
public class TurtleOverlayRecipe extends CustomShapelessRecipe {
    private final ResourceLocation overlay;

    public TurtleOverlayRecipe(ShapelessRecipeSpec spec, ResourceLocation overlay) {
        super(spec);
        this.overlay = overlay;
    }

    private static ItemStack make(ItemStack stack, TurtleItem turtle, ResourceLocation overlay) {
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
            if (stack.getItem() instanceof TurtleItem turtle) return make(stack, turtle, overlay);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<TurtleOverlayRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE_OVERLAY.get();
    }

    public static class Serialiser implements RecipeSerializer<TurtleOverlayRecipe> {
        private static final Codec<TurtleOverlayRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShapelessRecipeSpec.CODEC.forGetter(CustomShapelessRecipe::toSpec),
            ResourceLocation.CODEC.fieldOf("overlay").forGetter(x -> x.overlay)
        ).apply(instance, TurtleOverlayRecipe::new));
        @Override
        public Codec<TurtleOverlayRecipe> codec() {
            return CODEC;
        }

        @Override
        public TurtleOverlayRecipe fromNetwork(FriendlyByteBuf buffer) {
            var recipe = ShapelessRecipeSpec.fromNetwork(buffer);
            var overlay = buffer.readResourceLocation();
            return new TurtleOverlayRecipe(recipe, overlay);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TurtleOverlayRecipe recipe) {
            recipe.toSpec().toNetwork(buffer);
            buffer.writeResourceLocation(recipe.overlay);
        }
    }
}
