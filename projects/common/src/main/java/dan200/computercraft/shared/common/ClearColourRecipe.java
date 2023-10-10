// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Craft a wet sponge with a {@linkplain IColouredItem dyable item} to remove its dye.
 */
public final class ClearColourRecipe extends CustomRecipe {
    public ClearColourRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        var hasColourable = false;
        var hasSponge = false;
        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof IColouredItem colourable) {
                if (hasColourable) return false;
                if (colourable.getColour(stack) == -1) return false;
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
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        var colourable = ItemStack.EMPTY;

        for (var i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.getItem() instanceof IColouredItem) colourable = stack;
        }

        if (colourable.isEmpty()) return ItemStack.EMPTY;

        var stack = ((IColouredItem) colourable.getItem()).withColour(colourable, -1);
        stack.setCount(1);
        return stack;
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
