// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ColourableRecipe extends CustomRecipe {
    public ColourableRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
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

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
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
    public RecipeSerializer<ColourableRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.DYEABLE_ITEM.get();
    }
}
