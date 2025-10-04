// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TurtleInventoryCrafting {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 3;

    private TurtleInventoryCrafting() {
    }

    private static @Nullable FoundRecipe tryCrafting(ServerLevel level, Container inventory, int xStart, int yStart) {
        // Check the non-relevant parts of the inventory are empty
        for (var x = 0; x < TurtleBlockEntity.INVENTORY_WIDTH; x++) {
            for (var y = 0; y < TurtleBlockEntity.INVENTORY_HEIGHT; y++) {
                if (x < xStart || x >= xStart + WIDTH || y < yStart || y >= yStart + HEIGHT) {
                    if (!inventory.getItem(x + y * TurtleBlockEntity.INVENTORY_WIDTH).isEmpty()) {
                        return null;
                    }
                }
            }
        }

        List<ItemStack> items = new AbstractList<>() {
            @Override
            public ItemStack get(int index) {
                var x = xStart + index % WIDTH;
                var y = yStart + index / WIDTH;
                return x >= 0 && x < TurtleBlockEntity.INVENTORY_WIDTH && y >= 0 && y < TurtleBlockEntity.INVENTORY_HEIGHT
                    ? inventory.getItem(x + y * TurtleBlockEntity.INVENTORY_WIDTH)
                    : ItemStack.EMPTY;
            }

            @Override
            public int size() {
                return WIDTH * HEIGHT;
            }
        };
        var input = CraftingInput.of(WIDTH, HEIGHT, items);
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        return recipe == null ? null : new FoundRecipe(recipe.value(), items, xStart, yStart);
    }

    @Nullable
    public static List<ItemStack> craft(ITurtleAccess turtle, int maxCount) {
        if (!(turtle.getLevel() instanceof ServerLevel level)) return null;

        var inventory = turtle.getInventory();

        // Find out what we can craft
        var candidate = tryCrafting(level, inventory, 0, 0);
        if (candidate == null) candidate = tryCrafting(level, inventory, 0, 1);
        if (candidate == null) candidate = tryCrafting(level, inventory, 1, 0);
        if (candidate == null) candidate = tryCrafting(level, inventory, 1, 1);
        if (candidate == null) return null;

        // Special case: craft(0) just returns an empty list if crafting was possible
        if (maxCount == 0) return List.of();

        var recipe = candidate.recipe();
        var items = candidate.items();

        var results = new ArrayList<ItemStack>();
        for (var i = 0; i < maxCount; i++) {
            var offsetInput = CraftingInput.ofPositioned(WIDTH, HEIGHT, items);
            var input = offsetInput.input();

            if (!recipe.matches(input, level)) break;

            var result = recipe.assemble(input, level.registryAccess());
            if (result.isEmpty()) break;
            results.add(result);

            result.onCraftedBySystem(level);

            // Remove items from the inventory, and add back the remainders.
            var xStart = candidate.xStart() + offsetInput.left();
            var yStart = candidate.yStart() + offsetInput.top();
            var remainders = recipe.getRemainingItems(input);
            for (var y = 0; y < input.height(); y++) {
                for (var x = 0; x < input.width(); x++) {
                    var slot = xStart + x + (y + yStart) * TurtleBlockEntity.INVENTORY_WIDTH;
                    var existing = inventory.getItem(slot);
                    var remainder = remainders.get(x + y * input.width());

                    if (!existing.isEmpty()) {
                        inventory.removeItem(slot, 1);
                        existing = inventory.getItem(slot);
                    }

                    if (remainder.isEmpty()) continue;

                    // Either update the current stack or add it to the remainder list (to be inserted into the inventory
                    // afterwards).
                    if (existing.isEmpty()) {
                        inventory.setItem(slot, remainder);
                    } else if (ItemStack.isSameItemSameComponents(existing, remainder)) {
                        existing.grow(remainder.getCount());
                    } else {
                        results.add(remainder);
                    }
                }
            }
        }

        return Collections.unmodifiableList(results);
    }

    private record FoundRecipe(CraftingRecipe recipe, List<ItemStack> items, int xStart, int yStart) {
    }
}
