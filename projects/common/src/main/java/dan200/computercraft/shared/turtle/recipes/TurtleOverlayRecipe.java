// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.BasicRecipeSerialiser;
import dan200.computercraft.shared.recipe.CustomShapelessRecipe;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A {@link ShapelessRecipe} which sets the {@linkplain TurtleItem#getOverlay(ItemStack)} turtle's overlay} instead.
 */
public class TurtleOverlayRecipe extends CustomShapelessRecipe {
    private static final MapCodec<TurtleOverlayRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShapelessRecipeSpec.CODEC.forGetter(CustomShapelessRecipe::toSpec),
        ResourceLocation.CODEC.fieldOf("overlay").forGetter(x -> x.overlay)
    ).apply(instance, TurtleOverlayRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, TurtleOverlayRecipe> STREAM_CODEC = StreamCodec.composite(
        ShapelessRecipeSpec.STREAM_CODEC, CustomShapelessRecipe::toSpec,
        ResourceLocation.STREAM_CODEC, x -> x.overlay,
        TurtleOverlayRecipe::new
    );

    private final ResourceLocation overlay;

    public TurtleOverlayRecipe(ShapelessRecipeSpec spec, ResourceLocation overlay) {
        super(spec);
        this.overlay = overlay;
    }

    private static ItemStack make(ItemStack stack, ResourceLocation overlay) {
        return DataComponentUtil.createResult(stack, ModRegistry.DataComponents.OVERLAY.get(), overlay);
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
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

    public static RecipeSerializer<TurtleOverlayRecipe> serialiser() {
        return new BasicRecipeSerialiser<>(CODEC, STREAM_CODEC);
    }
}
