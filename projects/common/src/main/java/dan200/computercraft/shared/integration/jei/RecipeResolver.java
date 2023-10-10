// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.Collections;
import java.util.List;

class RecipeResolver implements IRecipeManagerPlugin {
    private final UpgradeRecipeGenerator<CraftingRecipe> resolver = new UpgradeRecipeGenerator<>(x -> x);

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        var value = focus.getTypedValue().getIngredient();
        if (!(value instanceof ItemStack stack)) return Collections.emptyList();

        return switch (focus.getRole()) {
            case INPUT ->
                stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem || resolver.isUpgrade(stack)
                    ? Collections.singletonList(RecipeTypes.CRAFTING)
                    : Collections.emptyList();
            case OUTPUT -> stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem
                ? Collections.singletonList(RecipeTypes.CRAFTING)
                : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        if (!(focus.getTypedValue().getIngredient() instanceof ItemStack stack) || recipeCategory.getRecipeType() != RecipeTypes.CRAFTING) {
            return Collections.emptyList();
        }

        return switch (focus.getRole()) {
            case INPUT -> cast(resolver.findRecipesWithInput(stack));
            case OUTPUT -> cast(resolver.findRecipesWithOutput(stack));
            default -> Collections.emptyList();
        };
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return Collections.emptyList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, U> List<T> cast(List<U> from) {
        return (List) from;
    }
}
