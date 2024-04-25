// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Craft a wet sponge with a {@linkplain ComputerCraftTags.Items#DYEABLE dyable item} to remove its dye.
 */
public final class ClearColourRecipe extends CustomRecipe {
    public ClearColourRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        var hasColourable = false;
        var hasSponge = false;
        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
                if (hasColourable) return false;
                if (!stack.has(DataComponents.DYED_COLOR)) return false;
                hasColourable = true;
            } else if (stack.getItem() == Items.WET_SPONGE) {
                if (hasSponge) return false;
                hasSponge = true;
            } else {
                return false;
            }
        }

        return hasColourable && hasSponge;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, HolderLookup.Provider registryAccess) {
        var colourable = ItemStack.EMPTY;

        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.is(ComputerCraftTags.Items.DYEABLE)) colourable = stack;
        }

        if (colourable.isEmpty()) return ItemStack.EMPTY;

        return DataComponentUtil.createResult(colourable, DataComponents.DYED_COLOR, null);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (var i = 0; i < remaining.size(); i++) {
            if (container.getItem(i).getItem() == Items.WET_SPONGE) remaining.set(i, new ItemStack(Items.WET_SPONGE));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 2;
    }

    @Override
    public RecipeSerializer<ClearColourRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.DYEABLE_ITEM_CLEAR.get();
    }
}
