// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ColourableRecipe extends CustomRecipe {
    public ColourableRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var hasColourable = false;
        var hasDye = false;
        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
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
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        var colourable = ItemStack.EMPTY;

        var tracker = new ColourTracker();

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
                colourable = stack;
            } else {
                var dye = ColourUtils.getStackColour(stack);
                if (dye != null) tracker.addColour(dye);
            }
        }

        return colourable.isEmpty()
            ? ItemStack.EMPTY
            : DataComponentUtil.createResult(colourable, DataComponents.DYED_COLOR, new DyedItemColor(tracker.getColour(), false));

    }

    @Override
    public RecipeSerializer<ColourableRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.DYEABLE_ITEM.get();
    }
}
