// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static dan200.computercraft.shared.integration.RecipeModHelpers.*;

/**
 * Provides dynamic recipe and usage information for upgraded turtle and pocket computers. This is intended to be
 * consumed by our recipe mod plugins (for example JEI).
 *
 * @param <T> The type the recipe mod uses for recipes.
 * @see RecipeModHelpers
 */
public class UpgradeRecipeGenerator<T> {
    private static final SlotDisplay CRAFTING_STATION = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);

    private final Function<RecipeDisplay, T> wrap;
    private final HolderLookup.Provider registries;

    private final Map<Item, List<UpgradeInfo>> upgradeItemLookup = new HashMap<>();
    private final List<UpgradeInfo> pocketUpgrades = new ArrayList<>();
    private final List<UpgradeInfo> turtleUpgrades = new ArrayList<>();
    private boolean initialised = false;

    public UpgradeRecipeGenerator(Function<RecipeDisplay, T> wrap, HolderLookup.Provider registries) {
        this.wrap = wrap;
        this.registries = registries;
    }

    /**
     * Build a cache of items which are used for turtle and pocket computer upgrades.
     */
    private void setupCache() {
        if (initialised) return;
        initialised = true;

        forEachRegistry(registries, ITurtleUpgrade.REGISTRY, holder -> {
            var upgrade = holder.value();
            var stack = upgrade.getCraftingItem();
            if (stack.isEmpty()) return;

            var info = new UpgradeInfo(stack, upgrade, holder, null);
            upgradeItemLookup.computeIfAbsent(stack.getItem(), k -> new ArrayList<>(1)).add(info);
            turtleUpgrades.add(info);
        });

        forEachRegistry(registries, IPocketUpgrade.REGISTRY, holder -> {
            var upgrade = holder.value();
            var stack = upgrade.getCraftingItem();
            if (stack.isEmpty()) return;

            var info = new UpgradeInfo(stack, upgrade, null, holder);
            upgradeItemLookup.computeIfAbsent(stack.getItem(), k -> new ArrayList<>(1)).add(info);
            pocketUpgrades.add(info);
        });
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

        if (stack.getItem() instanceof TurtleItem) {
            // Suggest possible upgrades which can be applied to this turtle
            var left = TurtleItem.getUpgradeWithData(stack, TurtleSide.LEFT);
            var right = TurtleItem.getUpgradeWithData(stack, TurtleSide.RIGHT);
            if (left != null && right != null) return List.of();

            List<T> recipes = new ArrayList<>();
            var ingredient = new SlotDisplay.ItemStackSlotDisplay(stack);
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
            var ingredient = new SlotDisplay.ItemStackSlotDisplay(stack);
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
        if (stack.getItem() instanceof TurtleItem) {
            List<T> recipes = new ArrayList<>(0);

            var left = TurtleItem.getUpgradeWithData(stack, TurtleSide.LEFT);
            var right = TurtleItem.getUpgradeWithData(stack, TurtleSide.RIGHT);

            // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
            if (left != null) {
                recipes.add(turtle(
                    new SlotDisplay.ItemStackSlotDisplay(turtleWith(stack, null, right)),
                    new SlotDisplay.ItemStackSlotDisplay(left.getUpgradeItem()),
                    stack
                ));
            }

            if (right != null) {
                recipes.add(turtle(
                    new SlotDisplay.ItemStackSlotDisplay(right.getUpgradeItem()),
                    new SlotDisplay.ItemStackSlotDisplay(turtleWith(stack, left, null)),
                    stack
                ));
            }

            return Collections.unmodifiableList(recipes);
        } else if (stack.getItem() instanceof PocketComputerItem) {
            List<T> recipes = new ArrayList<>(0);

            var back = PocketComputerItem.getUpgradeWithData(stack);
            if (back != null) {
                recipes.add(pocket(new SlotDisplay.ItemStackSlotDisplay(back.getUpgradeItem()), new SlotDisplay.ItemStackSlotDisplay(pocketWith(stack, null)), stack));
            }

            return Collections.unmodifiableList(recipes);
        } else {
            return List.of();
        }
    }

    private static ItemStack turtleWith(ItemStack stack, @Nullable UpgradeData<ITurtleUpgrade> left, @Nullable UpgradeData<ITurtleUpgrade> right) {
        var newStack = stack.copyWithCount(1);
        newStack.set(ModRegistry.DataComponents.LEFT_TURTLE_UPGRADE.get(), left);
        newStack.set(ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), right);
        return newStack;
    }

    private static ItemStack pocketWith(ItemStack stack, @Nullable UpgradeData<IPocketUpgrade> back) {
        var newStack = stack.copyWithCount(1);
        newStack.set(ModRegistry.DataComponents.POCKET_UPGRADE.get(), back);
        return newStack;
    }

    private T pocket(SlotDisplay upgrade, SlotDisplay pocketComputer, ItemStack result) {
        return wrap.apply(new ShapedCraftingRecipeDisplay(
            1, 2, List.of(upgrade, pocketComputer), new SlotDisplay.ItemStackSlotDisplay(result), CRAFTING_STATION
        ));
    }

    private T turtle(SlotDisplay left, SlotDisplay right, ItemStack result) {
        return wrap.apply(new ShapedCraftingRecipeDisplay(
            2, 1, List.of(left, right), new SlotDisplay.ItemStackSlotDisplay(result), CRAFTING_STATION
        ));
    }

    private class UpgradeInfo {
        final ItemStack stack;
        final SlotDisplay ingredient;
        final @Nullable Holder.Reference<ITurtleUpgrade> turtle;
        final @Nullable Holder.Reference<IPocketUpgrade> pocket;
        final UpgradeBase upgrade;
        private @Nullable ArrayList<T> recipes;

        UpgradeInfo(ItemStack stack, UpgradeBase upgrade, @Nullable Holder.Reference<ITurtleUpgrade> turtle, @Nullable Holder.Reference<IPocketUpgrade> pocket) {
            this.stack = stack;
            ingredient = new SlotDisplay.ItemStackSlotDisplay(stack);
            this.turtle = turtle;
            this.pocket = pocket;
            this.upgrade = upgrade;
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
                        new SlotDisplay.ItemSlotDisplay(turtleItem),
                        DataComponentUtil.createStack(turtleItem, ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), UpgradeData.ofDefault(turtle))
                    ));
                }
            }

            if (pocket != null) {
                for (var pocketSupplier : POCKET_COMPUTERS) {
                    var pocketItem = pocketSupplier.get();
                    recipes.add(pocket(
                        ingredient,
                        new SlotDisplay.ItemSlotDisplay(pocketItem),
                        DataComponentUtil.createStack(pocketItem, ModRegistry.DataComponents.POCKET_UPGRADE.get(), UpgradeData.ofDefault(pocket))
                    ));
                }
            }

            recipes.trimToSize();
            return recipes;
        }
    }
}
