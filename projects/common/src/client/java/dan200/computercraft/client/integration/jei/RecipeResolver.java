// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.jei;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

class RecipeResolver implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
    /**
     * We need to generate unique ids for each recipe, as JEI will attempt to deduplicate them otherwise.
     */
    private int nextId = 0;

    private final UpgradeRecipeGenerator<RecipeHolder<CraftingRecipe>> resolver;

    RecipeResolver(HolderLookup.Provider registries) {
        resolver = new UpgradeRecipeGenerator<>(x -> new RecipeHolder<>(
            ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "upgrade_" + nextId++)),
            new CraftingWrapper(x)
        ), registries);
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return input.getIngredient() instanceof ItemStack stack
            && (stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem || resolver.isUpgrade(stack));
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return output.getIngredient() instanceof ItemStack stack
            && (stack.getItem() instanceof TurtleItem || stack.getItem() instanceof PocketComputerItem);
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
        return input.getIngredient() instanceof ItemStack stack ? resolver.findRecipesWithInput(stack) : List.of();
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
        return output.getIngredient() instanceof ItemStack stack ? resolver.findRecipesWithOutput(stack) : List.of();
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
        return List.of();
    }

    private record CraftingWrapper(RecipeDisplay recipes) implements CraftingRecipe {
        @Override
        public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
            throw new IllegalStateException("Should not serialise CraftingWrapper");
        }

        @Override
        public CraftingBookCategory category() {
            return CraftingBookCategory.MISC;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return false;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public PlacementInfo placementInfo() {
            return PlacementInfo.NOT_PLACEABLE;
        }

        @Override
        public List<RecipeDisplay> display() {
            return List.of(recipes);
        }
    }
}
