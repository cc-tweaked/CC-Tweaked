// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

class RecipeResolver implements IRecipeManagerPlugin {
    private static final ResourceLocation RECIPE_ID = new ResourceLocation(ComputerCraftAPI.MOD_ID, "upgrade");
    private final UpgradeRecipeGenerator<RecipeHolder<CraftingRecipe>> resolver = new UpgradeRecipeGenerator<>(x -> new RecipeHolder<>(RECIPE_ID, x));

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        var value = focus.getTypedValue().getIngredient();
        if (!(value instanceof ItemStack stack)) return List.of();

        return switch (focus.getRole()) {
            case INPUT ->
                stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem || resolver.isUpgrade(stack)
                    ? List.of(RecipeTypes.CRAFTING)
                    : List.of();
            case OUTPUT -> stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem
                ? List.of(RecipeTypes.CRAFTING)
                : List.of();
            default -> List.of();
        };
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        if (!(focus.getTypedValue().getIngredient() instanceof ItemStack stack) || recipeCategory.getRecipeType() != RecipeTypes.CRAFTING) {
            return List.of();
        }

        return switch (focus.getRole()) {
            case INPUT -> cast(RecipeTypes.CRAFTING, resolver.findRecipesWithInput(stack));
            case OUTPUT -> cast(RecipeTypes.CRAFTING, resolver.findRecipesWithOutput(stack));
            default -> List.of();
        };
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return List.of();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, U> List<T> cast(RecipeType<U> ignoredType, List<U> from) {
        return (List) from;
    }
}
