/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

class RecipeResolver implements IRecipeManagerPlugin {
    private final UpgradeRecipeGenerator<CraftingRecipe> resolver = new UpgradeRecipeGenerator<>(x -> x);

    @Nonnull
    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        var value = focus.getTypedValue().getIngredient();
        if (!(value instanceof ItemStack stack)) return Collections.emptyList();

        return switch (focus.getRole()) {
            case INPUT ->
                stack.getItem() instanceof ItemTurtle || stack.getItem() instanceof ItemPocketComputer || resolver.isUpgrade(stack)
                    ? Collections.singletonList(RecipeTypes.CRAFTING)
                    : Collections.emptyList();
            case OUTPUT -> stack.getItem() instanceof ItemTurtle || stack.getItem() instanceof ItemPocketComputer
                ? Collections.singletonList(RecipeTypes.CRAFTING)
                : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    @Nonnull
    @Override
    public <T, V> List<T> getRecipes(@Nonnull IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        if (!(focus.getTypedValue().getIngredient() instanceof ItemStack stack) || recipeCategory.getRecipeType() != RecipeTypes.CRAFTING) {
            return Collections.emptyList();
        }

        return switch (focus.getRole()) {
            case INPUT -> cast(resolver.findRecipesWithInput(stack));
            case OUTPUT -> cast(resolver.findRecipesWithOutput(stack));
            default -> Collections.emptyList();
        };
    }

    @Nonnull
    @Override
    public <T> List<T> getRecipes(@Nonnull IRecipeCategory<T> recipeCategory) {
        return Collections.emptyList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, U> List<T> cast(List<U> from) {
        return (List) from;
    }
}
