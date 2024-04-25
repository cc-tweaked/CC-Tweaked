// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.items.AbstractComputerItem;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.recipe.CustomShapedRecipe;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * A recipe which converts a computer from one form into another.
 */
public final class ComputerConvertRecipe extends CustomShapedRecipe {
    private final Item result;

    public ComputerConvertRecipe(ShapedRecipeSpec recipe) {
        super(recipe);
        this.result = recipe.result().getItem();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
        // Find our computer item and copy the components across.
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (isComputerItem(stack.getItem())) {
                var newStack = new ItemStack(result);
                newStack.applyComponents(stack.getComponentsPatch());
                return newStack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ComputerConvertRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.COMPUTER_CONVERT.get();
    }

    private static boolean isComputerItem(Item item) {
        // TODO: Make this a little more general. Either with a tag, or a predicate on the recipe itself?
        return item instanceof AbstractComputerItem || item instanceof PocketComputerItem;
    }
}
