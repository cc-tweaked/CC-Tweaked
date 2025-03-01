// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class DiskRecipe extends CustomRecipe {
    private final Ingredient redstone;

    public DiskRecipe(CraftingBookCategory category) {
        super(category);
        redstone = PlatformHelper.get().getRecipeIngredients().redstone();
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var paperFound = false;
        var redstoneFound = false;

        for (var i = 0; i < inv.size(); i++) {
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
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        var tracker = new ColourTracker();

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.getItem() != Items.PAPER && !redstone.test(stack)) {
                var dye = ColourUtils.getStackColour(stack);
                if (dye != null) tracker.addColour(dye);
            }
        }

        return DataComponentUtil.createStack(ModRegistry.Items.DISK.get(), DataComponents.DYED_COLOR, new DyedItemColor(tracker.hasColour() ? tracker.getColour() : Colour.BLUE.getHex(), false));
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 2 && y >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return DataComponentUtil.createStack(ModRegistry.Items.DISK.get(), DataComponents.DYED_COLOR, new DyedItemColor(Colour.BLUE.getHex(), false));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.DISK.get();
    }
}
