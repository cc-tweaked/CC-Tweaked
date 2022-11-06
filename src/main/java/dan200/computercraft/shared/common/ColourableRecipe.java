/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class ColourableRecipe extends CustomRecipe {
    public ColourableRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level world) {
        var hasColourable = false;
        var hasDye = false;
        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof IColouredItem) {
                if (hasColourable) return false;
                hasColourable = true;
            } else if (ColourUtils.getStackColour(stack) != null) {
                hasDye = true;
            } else {
                return false;
            }
        }

        return hasColourable && hasDye;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inv) {
        var colourable = ItemStack.EMPTY;

        var tracker = new ColourTracker();

        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof IColouredItem) {
                colourable = stack;
            } else {
                var dye = ColourUtils.getStackColour(stack);
                if (dye != null) tracker.addColour(dye);
            }
        }

        if (colourable.isEmpty()) return ItemStack.EMPTY;

        var stack = ((IColouredItem) colourable.getItem()).withColour(colourable, tracker.getColour());
        stack.setCount(1);
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 2 && y >= 2;
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.DYEABLE_ITEM.get();
    }
}
