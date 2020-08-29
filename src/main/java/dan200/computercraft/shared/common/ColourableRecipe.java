/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.common;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ColourableRecipe extends SpecialCraftingRecipe {
    public static final RecipeSerializer<?> SERIALIZER = new SpecialRecipeSerializer<>(ColourableRecipe::new);

    public ColourableRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(@Nonnull CraftingInventory inv, @Nonnull World world) {
        boolean hasColourable = false;
        boolean hasDye = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof IColouredItem) {
                if (hasColourable) {
                    return false;
                }
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
    public ItemStack craft(@Nonnull CraftingInventory inv) {
        ItemStack colourable = ItemStack.EMPTY;

        ColourTracker tracker = new ColourTracker();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof IColouredItem) {
                colourable = stack;
            } else {
                DyeColor dye = ColourUtils.getStackColour(stack);
                if (dye == null) {
                    continue;
                }

                Colour colour = Colour.fromInt(15 - dye.getId());
                tracker.addColour(colour.getR(), colour.getG(), colour.getB());
            }
        }

        if (colourable.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ((IColouredItem) colourable.getItem()).withColour(colourable, tracker.getColour());
    }

    @Override
    public boolean fits(int x, int y) {
        return x >= 2 && y >= 2;
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
