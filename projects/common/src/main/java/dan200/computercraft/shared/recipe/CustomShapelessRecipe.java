// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A custom version of {@link ShapelessRecipe}, which can be converted to and from a {@link ShapelessRecipeSpec}.
 */
public abstract class CustomShapelessRecipe extends AbstractCraftingRecipe {
    private final ShapelessRecipeSpec spec;
    private @Nullable PlacementInfo placementInfo;

    protected CustomShapelessRecipe(ShapelessRecipeSpec recipe) {
        super(recipe.properties());
        this.spec = recipe;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(spec.ingredients());
        return placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapelessCraftingRecipeDisplay(
                spec.ingredients().stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(spec.result()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        var ingredients = spec.ingredients();
        if (input.ingredientCount() != ingredients.size()) return false;
        // Fast-path with a single item - just check the ingredient matches.
        if (input.size() == 1 && ingredients.size() == 1) return ingredients.getFirst().test(input.getItem(0));
        // Otherwise check the stacked contents.
        return input.stackedContents().canCraft(placementInfo().unpackedIngredients(), null);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return spec.result().copy();
    }

    protected final ShapelessRecipeSpec toSpec() {
        return spec;
    }

    @Override
    public abstract RecipeSerializer<? extends CustomShapelessRecipe> getSerializer();

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> serialiser(Function<ShapelessRecipeSpec, T> factory) {
        return new BasicRecipeSerialiser<>(
            ShapelessRecipeSpec.CODEC.xmap(factory, CustomShapelessRecipe::toSpec),
            ShapelessRecipeSpec.STREAM_CODEC.map(factory, CustomShapelessRecipe::toSpec)
        );
    }
}
