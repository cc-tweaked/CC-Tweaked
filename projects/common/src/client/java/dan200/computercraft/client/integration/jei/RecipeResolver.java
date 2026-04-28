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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;
import java.util.Optional;

class RecipeResolver implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
    private static final SlotDisplay CRAFTING_STATION = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);

    /**
     * We need to generate unique ids for each recipe, as JEI will attempt to deduplicate them otherwise.
     */
    private int nextId = 0;

    private final UpgradeRecipeGenerator<RecipeHolder<CraftingRecipe>> resolver;

    RecipeResolver(HolderLookup.Provider registries) {
        resolver = new UpgradeRecipeGenerator<>((width, height, inputs, result) -> {
            var id = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "upgrade_" + nextId++));
            var pattern = new ShapedRecipePattern(
                width, height,
                inputs.stream().map(x -> Optional.of(Ingredient.of(x.item().value()))).toList(),
                Optional.empty()
            );
            var display = new ShapedCraftingRecipeDisplay(
                width, height,
                inputs.stream().<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList(),
                new SlotDisplay.ItemStackSlotDisplay(result),
                CRAFTING_STATION
            );
            return new RecipeHolder<>(id, new CraftingWrapper(pattern, result, display));
        }, registries);
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

    private static final class CraftingWrapper extends ShapedRecipe {
        private static final CommonInfo COMMON_INFO = new CommonInfo(false);
        private static final CraftingBookInfo BOOK_INFO = new CraftingBookInfo(CraftingBookCategory.MISC, "");

        private final ShapedCraftingRecipeDisplay display;

        CraftingWrapper(ShapedRecipePattern pattern, ItemStackTemplate result, ShapedCraftingRecipeDisplay display) {
            super(COMMON_INFO, BOOK_INFO, pattern, result);
            this.display = display;
        }

        @Override
        public List<RecipeDisplay> display() {
            return List.of(display);
        }
    }
}
