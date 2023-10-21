// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static dan200.computercraft.shared.integration.RecipeModHelpers.POCKET_COMPUTERS;
import static dan200.computercraft.shared.integration.RecipeModHelpers.TURTLES;

/**
 * Provides dynamic recipe and usage information for upgraded turtle and pocket computers. This is intended to be
 * consumed by our recipe mod plugins (for example JEI).
 *
 * @param <T> The type the recipe mod uses for recipes.
 * @see RecipeModHelpers
 */
public class UpgradeRecipeGenerator<T> {
    private static final ResourceLocation TURTLE_UPGRADE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_upgrade");
    private static final ResourceLocation POCKET_UPGRADE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "pocket_upgrade");

    private final Function<CraftingRecipe, T> wrap;

    private final Map<Item, List<UpgradeInfo>> upgradeItemLookup = new HashMap<>();
    private final List<UpgradeInfo> pocketUpgrades = new ArrayList<>();
    private final List<UpgradeInfo> turtleUpgrades = new ArrayList<>();
    private boolean initialised = false;

    public UpgradeRecipeGenerator(Function<CraftingRecipe, T> wrap) {
        this.wrap = wrap;
    }

    /**
     * Build a cache of items which are used for turtle and pocket computer upgrades.
     */
    private void setupCache() {
        if (initialised) return;
        initialised = true;

        for (var upgrade : TurtleUpgrades.instance().getUpgrades()) {
            var stack = upgrade.getCraftingItem();
            if (stack.isEmpty()) return;

            var info = new UpgradeInfo(stack, upgrade);
            upgradeItemLookup.computeIfAbsent(stack.getItem(), k -> new ArrayList<>(1)).add(info);
            turtleUpgrades.add(info);
        }

        for (var upgrade : PocketUpgrades.instance().getUpgrades()) {
            var stack = upgrade.getCraftingItem();
            if (stack.isEmpty()) return;

            var info = new UpgradeInfo(stack, upgrade);
            upgradeItemLookup.computeIfAbsent(stack.getItem(), k -> new ArrayList<>(1)).add(info);
            pocketUpgrades.add(info);
        }
    }

    /**
     * Check if this item is usable as a turtle or pocket computer upgrade.
     *
     * @param stack The stack to check.
     * @return Whether the item is an upgrade.
     */
    public boolean isUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return false;

        setupCache();
        var upgrades = upgradeItemLookup.get(stack.getItem());
        if (upgrades == null) return false;

        for (var upgrade : upgrades) {
            var craftingStack = upgrade.stack;
            if (!craftingStack.isEmpty() && craftingStack.getItem() == stack.getItem() && upgrade.upgrade.isItemSuitable(stack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find all usages of the given stack.
     *
     * @param stack The stack to find usages of.
     * @return All upgrade recipes which take the current stack as an input.
     */
    public List<T> findRecipesWithInput(ItemStack stack) {
        setupCache();

        if (stack.getItem() instanceof TurtleItem item) {
            // Suggest possible upgrades which can be applied to this turtle
            var left = item.getUpgradeWithData(stack, TurtleSide.LEFT);
            var right = item.getUpgradeWithData(stack, TurtleSide.RIGHT);
            if (left != null && right != null) return List.of();

            List<T> recipes = new ArrayList<>();
            var ingredient = Ingredient.of(stack);
            for (var upgrade : turtleUpgrades) {
                if (upgrade.turtle == null) throw new NullPointerException();

                // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
                if (left == null) {
                    recipes.add(turtle(ingredient, upgrade.ingredient, turtleWith(stack, UpgradeData.ofDefault(upgrade.turtle), right)));
                }

                if (right == null) {
                    recipes.add(turtle(upgrade.ingredient, ingredient, turtleWith(stack, left, UpgradeData.ofDefault(upgrade.turtle))));
                }
            }

            return Collections.unmodifiableList(recipes);
        } else if (stack.getItem() instanceof PocketComputerItem) {
            // Suggest possible upgrades which can be applied to this turtle
            var back = PocketComputerItem.getUpgrade(stack);
            if (back != null) return List.of();

            List<T> recipes = new ArrayList<>();
            var ingredient = Ingredient.of(stack);
            for (var upgrade : pocketUpgrades) {
                if (upgrade.pocket == null) throw new NullPointerException();
                recipes.add(pocket(upgrade.ingredient, ingredient, pocketWith(stack, UpgradeData.ofDefault(upgrade.pocket))));
            }

            return Collections.unmodifiableList(recipes);
        } else {
            // If this item is usable as an upgrade, find all possible recipes.
            var upgrades = upgradeItemLookup.get(stack.getItem());
            if (upgrades == null) return List.of();

            List<T> recipes = null;
            var multiple = false;
            for (var upgrade : upgrades) {
                var craftingStack = upgrade.stack;
                if (craftingStack.isEmpty() || craftingStack.getItem() != stack.getItem() || !upgrade.upgrade.isItemSuitable(stack)) {
                    continue;
                }

                if (recipes == null) {
                    recipes = upgrade.getRecipes();
                } else {
                    if (!multiple) {
                        multiple = true;
                        recipes = new ArrayList<>(recipes);
                    }
                    recipes.addAll(upgrade.getRecipes());
                }
            }

            return recipes == null ? List.of() : Collections.unmodifiableList(recipes);
        }
    }

    /**
     * Find all recipes for the given stack.
     *
     * @param stack The stack to find recipes of.
     * @return All upgrade recipes which produce the stack as an output.
     */
    public List<T> findRecipesWithOutput(ItemStack stack) {
        // Find which upgrade this item currently has, and so how we could build it.
        if (stack.getItem() instanceof TurtleItem item) {
            List<T> recipes = new ArrayList<>(0);

            var left = item.getUpgradeWithData(stack, TurtleSide.LEFT);
            var right = item.getUpgradeWithData(stack, TurtleSide.RIGHT);

            // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
            if (left != null) {
                recipes.add(turtle(
                    Ingredient.of(turtleWith(stack, null, right)),
                    Ingredient.of(left.getUpgradeItem()),
                    stack
                ));
            }

            if (right != null) {
                recipes.add(turtle(
                    Ingredient.of(right.getUpgradeItem()),
                    Ingredient.of(turtleWith(stack, left, null)),
                    stack
                ));
            }

            return Collections.unmodifiableList(recipes);
        } else if (stack.getItem() instanceof PocketComputerItem) {
            List<T> recipes = new ArrayList<>(0);

            var back = PocketComputerItem.getUpgradeWithData(stack);
            if (back != null) {
                recipes.add(pocket(Ingredient.of(back.getUpgradeItem()), Ingredient.of(pocketWith(stack, null)), stack));
            }

            return Collections.unmodifiableList(recipes);
        } else {
            return List.of();
        }
    }

    private static ItemStack turtleWith(ItemStack stack, @Nullable UpgradeData<ITurtleUpgrade> left, @Nullable UpgradeData<ITurtleUpgrade> right) {
        var item = (TurtleItem) stack.getItem();
        return item.create(
            item.getComputerID(stack), item.getLabel(stack), item.getColour(stack),
            left, right, item.getFuelLevel(stack), item.getOverlay(stack)
        );
    }

    private static ItemStack pocketWith(ItemStack stack, @Nullable UpgradeData<IPocketUpgrade> back) {
        var item = (PocketComputerItem) stack.getItem();
        return item.create(
            item.getComputerID(stack), item.getLabel(stack), item.getColour(stack), back
        );
    }

    private T pocket(Ingredient upgrade, Ingredient pocketComputer, ItemStack result) {
        return wrap.apply(new ShapedRecipe(POCKET_UPGRADE, "", CraftingBookCategory.MISC, 1, 2, NonNullList.of(Ingredient.EMPTY, upgrade, pocketComputer), result));
    }

    private T turtle(Ingredient left, Ingredient right, ItemStack result) {
        return wrap.apply(new ShapedRecipe(TURTLE_UPGRADE, "", CraftingBookCategory.MISC, 2, 1, NonNullList.of(Ingredient.EMPTY, left, right), result));
    }

    private class UpgradeInfo {
        final ItemStack stack;
        final Ingredient ingredient;
        final @Nullable ITurtleUpgrade turtle;
        final @Nullable IPocketUpgrade pocket;
        final UpgradeBase upgrade;
        private @Nullable ArrayList<T> recipes;

        UpgradeInfo(ItemStack stack, ITurtleUpgrade turtle) {
            this.stack = stack;
            ingredient = Ingredient.of(stack);
            upgrade = this.turtle = turtle;
            pocket = null;
        }

        UpgradeInfo(ItemStack stack, IPocketUpgrade pocket) {
            this.stack = stack;
            ingredient = Ingredient.of(stack);
            turtle = null;
            upgrade = this.pocket = pocket;
        }

        List<T> getRecipes() {
            var recipes = this.recipes;
            if (recipes != null) return recipes;

            recipes = this.recipes = new ArrayList<>(4);

            if (turtle != null) {
                for (var turtleSupplier : TURTLES) {
                    var turtleItem = turtleSupplier.get();
                    recipes.add(turtle(
                        ingredient, // Right upgrade, recipe on left
                        Ingredient.of(turtleItem.create(-1, null, -1, null, null, 0, null)),
                        turtleItem.create(-1, null, -1, null, UpgradeData.ofDefault(turtle), 0, null)
                    ));
                }
            }

            if (pocket != null) {
                for (var pocketSupplier : POCKET_COMPUTERS) {
                    var pocketItem = pocketSupplier.get();
                    recipes.add(pocket(
                        ingredient,
                        Ingredient.of(pocketItem.create(-1, null, -1, null)),
                        pocketItem.create(-1, null, -1, UpgradeData.ofDefault(pocket))
                    ));
                }
            }

            recipes.trimToSize();
            return recipes;
        }
    }
}
