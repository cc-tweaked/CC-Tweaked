// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.jei;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

class RecipeResolver implements IRecipeManagerPlugin {
    private final UpgradeRecipeGenerator<RecipeHolder<CraftingRecipe>> resolver;

    /**
     * We need to generate unique ids for each recipe, as JEI will attempt to deduplicate them otherwise.
     */
    private int nextId = 0;

    RecipeResolver(HolderLookup.Provider registries) {
        resolver = new UpgradeRecipeGenerator<>(
            x -> new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "upgrade_" + nextId++), x),
            registries
        );
    }

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

    @SuppressWarnings({ "unchecked", "rawtypes", "UnusedVariable" })
    private static <T, U> List<T> cast(RecipeType<U> ignoredType, List<U> from) {
        return (List) from;
    }
}
