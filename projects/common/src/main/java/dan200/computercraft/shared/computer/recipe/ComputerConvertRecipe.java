// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.recipe.CustomShapedRecipe;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends CustomShapedRecipe {
    public ComputerConvertRecipe(ResourceLocation identifier, ShapedRecipeSpec recipe) {
        super(identifier, recipe);
    }

    protected abstract ItemStack convert(IComputerItem item, ItemStack stack);

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        if (!super.matches(inventory, world)) return false;

        for (var i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof IComputerItem) return true;
        }

        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        // Find our computer item and convert it.
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.getItem() instanceof IComputerItem) return convert((IComputerItem) stack.getItem(), stack);
        }

        return ItemStack.EMPTY;
    }
}
