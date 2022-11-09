/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DiskRecipe extends CustomRecipe {
    private final Ingredient redstone;

    public DiskRecipe(ResourceLocation id) {
        super(id);
        redstone = PlatformHelper.get().getRecipeIngredients().redstone();
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        var paperFound = false;
        var redstoneFound = false;

        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);

            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.PAPER) {
                    if (paperFound) return false;
                    paperFound = true;
                } else if (redstone.test(stack)) {
                    if (redstoneFound) return false;
                    redstoneFound = true;
                } else if (ColourUtils.getStackColour(stack) == null) {
                    return false;
                }
            }
        }

        return redstoneFound && paperFound;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        var tracker = new ColourTracker();

        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.getItem() != Items.PAPER && !redstone.test(stack)) {
                var dye = ColourUtils.getStackColour(stack);
                if (dye != null) tracker.addColour(dye);
            }
        }

        return ItemDisk.createFromIDAndColour(-1, null, tracker.hasColour() ? tracker.getColour() : Colour.BLUE.getHex());
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 2 && y >= 2;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemDisk.createFromIDAndColour(-1, null, Colour.BLUE.getHex());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.DISK.get();
    }
}
