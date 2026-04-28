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
import dan200.computercraft.shared.pocket.core.PocketSide;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static dan200.computercraft.shared.integration.RecipeModHelpers.*;

/**
 * Provides dynamic recipe and usage information for upgraded turtle and pocket computers. This is intended to be
 * consumed by our recipe mod plugins (for example JEI).
 *
 * @param <T> The type the recipe mod uses for recipes.
 * @see RecipeModHelpers
 */
public class UpgradeRecipeGenerator<T> {
    private final RecipeFactory<T> recipeFactory;
    private final HolderLookup.Provider registries;

    private final Map<Item, List<UpgradeInfo>> upgradeItemLookup = new HashMap<>();
    private final List<UpgradeInfo> pocketUpgrades = new ArrayList<>();
    private final List<UpgradeInfo> turtleUpgrades = new ArrayList<>();
    private boolean initialised = false;

    public UpgradeRecipeGenerator(RecipeFactory<T> recipeFactory, HolderLookup.Provider registries) {
        this.recipeFactory = recipeFactory;
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

            var info = new UpgradeInfo(stack, upgrade, holder, null);
            upgradeItemLookup.computeIfAbsent(stack.item().value(), k -> new ArrayList<>(1)).add(info);
            turtleUpgrades.add(info);
        });

        forEachRegistry(registries, IPocketUpgrade.REGISTRY, holder -> {
            var upgrade = holder.value();
            var stack = upgrade.getCraftingItem();

            var info = new UpgradeInfo(stack, upgrade, null, holder);
            upgradeItemLookup.computeIfAbsent(stack.item().value(), k -> new ArrayList<>(1)).add(info);
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
            if (craftingStack.is(stack.getItem()) && upgrade.upgrade.isItemSuitable(stack)) return true;
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
            var ingredient = ItemStackTemplate.fromNonEmptyStack(stack);
            for (var upgrade : turtleUpgrades) {
                if (upgrade.turtle == null) throw new NullPointerException();

                // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
                if (left == null) {
                    recipes.add(turtle(ingredient, upgrade.stack, turtleWith(stack, UpgradeData.ofDefault(upgrade.turtle), right)));
                }

                if (right == null) {
                    recipes.add(turtle(upgrade.stack, ingredient, turtleWith(stack, left, UpgradeData.ofDefault(upgrade.turtle))));
                }
            }

            return Collections.unmodifiableList(recipes);
        } else if (stack.getItem() instanceof PocketComputerItem) {
            // Suggest possible upgrades which can be applied to this turtle
            var back = PocketComputerItem.getUpgradeWithData(stack, PocketSide.BACK);
            var bottom = PocketComputerItem.getUpgradeWithData(stack, PocketSide.BOTTOM);
            if (back != null && bottom != null) return List.of();

            List<T> recipes = new ArrayList<>();
            var ingredient = ItemStackTemplate.fromNonEmptyStack(stack);
            for (var upgrade : pocketUpgrades) {
                if (upgrade.pocket == null) throw new NullPointerException();

                if (back == null) {
                    recipes.add(pocket(upgrade.stack, ingredient, pocketWith(stack, UpgradeData.ofDefault(upgrade.pocket), bottom)));
                }

                if (bottom == null) {
                    recipes.add(pocket(ingredient, upgrade.stack, pocketWith(stack, back, UpgradeData.ofDefault(upgrade.pocket))));
                }
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
                if (!craftingStack.is(stack.getItem()) || !upgrade.upgrade.isItemSuitable(stack)) continue;

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
                    turtleWith(stack, null, right),
                    left.getUpgradeItem(),
                    ItemStackTemplate.fromNonEmptyStack(stack)
                ));
            }

            if (right != null) {
                recipes.add(turtle(
                    right.getUpgradeItem(),
                    turtleWith(stack, left, null),
                    ItemStackTemplate.fromNonEmptyStack(stack)
                ));
            }

            return Collections.unmodifiableList(recipes);
        } else if (stack.getItem() instanceof PocketComputerItem) {
            List<T> recipes = new ArrayList<>(0);

            var back = PocketComputerItem.getUpgradeWithData(stack, PocketSide.BACK);
            var bottom = PocketComputerItem.getUpgradeWithData(stack, PocketSide.BOTTOM);
            if (back != null) {
                recipes.add(pocket(
                    back.getUpgradeItem(),
                    pocketWith(stack, null, bottom),
                    ItemStackTemplate.fromNonEmptyStack(stack)
                ));
            }

            if (bottom != null) {
                recipes.add(pocket(
                    pocketWith(stack, back, null),
                    bottom.getUpgradeItem(),
                    ItemStackTemplate.fromNonEmptyStack(stack)
                ));
            }

            return Collections.unmodifiableList(recipes);
        } else {
            return List.of();
        }
    }

    private static ItemStackTemplate turtleWith(ItemStack stack, @Nullable UpgradeData<ITurtleUpgrade> left, @Nullable UpgradeData<ITurtleUpgrade> right) {
        var newStack = stack.copyWithCount(1);
        newStack.set(ModRegistry.DataComponents.LEFT_TURTLE_UPGRADE.get(), left);
        newStack.set(ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), right);
        return ItemStackTemplate.fromNonEmptyStack(newStack);
    }

    private static ItemStackTemplate pocketWith(ItemStack stack, @Nullable UpgradeData<IPocketUpgrade> back, @Nullable UpgradeData<IPocketUpgrade> bottom) {
        var newStack = stack.copyWithCount(1);
        newStack.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), back);
        newStack.set(ModRegistry.DataComponents.BOTTOM_POCKET_UPGRADE.get(), bottom);
        return ItemStackTemplate.fromNonEmptyStack(newStack);
    }

    private T pocket(ItemStackTemplate top, ItemStackTemplate bottom, ItemStackTemplate result) {
        return recipeFactory.create(1, 2, List.of(top, bottom), result);
    }

    private T turtle(ItemStackTemplate left, ItemStackTemplate right, ItemStackTemplate result) {
        return recipeFactory.create(2, 1, List.of(left, right), result);
    }

    private class UpgradeInfo {
        final ItemStackTemplate stack;
        final Holder.@Nullable Reference<ITurtleUpgrade> turtle;
        final Holder.@Nullable Reference<IPocketUpgrade> pocket;
        final UpgradeBase upgrade;
        private @Nullable List<T> recipes;

        UpgradeInfo(ItemStackTemplate stack, UpgradeBase upgrade, Holder.@Nullable Reference<ITurtleUpgrade> turtle, Holder.@Nullable Reference<IPocketUpgrade> pocket) {
            this.stack = stack;
            this.turtle = turtle;
            this.pocket = pocket;
            this.upgrade = upgrade;
        }

        List<T> getRecipes() {
            var recipes = this.recipes;
            return recipes != null ? recipes : (this.recipes = computeRecipes());
        }

        private List<T> computeRecipes() {
            List<T> recipes = new ArrayList<>(4);

            if (turtle != null) {
                for (var turtleSupplier : TURTLES) {
                    var turtleItem = turtleSupplier.get();
                    recipes.add(turtle(
                        stack, // Right upgrade, recipe on left
                        new ItemStackTemplate(turtleItem),
                        DataComponentUtil.createTemplate(turtleItem, ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), UpgradeData.ofDefault(turtle))
                    ));
                }
            }

            if (pocket != null) {
                for (var pocketSupplier : POCKET_COMPUTERS) {
                    var pocketItem = pocketSupplier.get();
                    recipes.add(pocket(
                        stack,
                        new ItemStackTemplate(pocketItem),
                        DataComponentUtil.createTemplate(pocketItem, ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), UpgradeData.ofDefault(pocket))
                    ));
                }
            }

            return List.copyOf(recipes);
        }
    }

    public interface RecipeFactory<T> {
        T create(int width, int height, List<ItemStackTemplate> ingredients, ItemStackTemplate result);
    }
}
