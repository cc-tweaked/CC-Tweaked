/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Represents a recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends ShapedRecipe {
    private final String group;
    private final ItemStack result;

    public ComputerConvertRecipe(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(identifier, group, category, width, height, ingredients, result);
        this.group = group;
        this.result = result;
    }

    public ItemStack getResultItem() {
        return result;
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

    @Override
    public String getGroup() {
        return group;
    }
}
