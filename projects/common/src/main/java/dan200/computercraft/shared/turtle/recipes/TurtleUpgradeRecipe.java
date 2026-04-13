// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.recipes;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class TurtleUpgradeRecipe extends CustomRecipe {
    private final HolderLookup<ITurtleUpgrade> upgradeRegistry;

    public TurtleUpgradeRecipe(HolderLookup<ITurtleUpgrade> upgradeRegistry) {
        this.upgradeRegistry = upgradeRegistry;
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        return !assemble(inventory).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        // Scan the grid for a row containing a turtle and 1 or 2 items
        var leftItem = ItemStack.EMPTY;
        var turtle = ItemStack.EMPTY;
        var rightItem = ItemStack.EMPTY;

        for (var y = 0; y < inventory.height(); y++) {
            if (turtle.isEmpty()) {
                // Search this row for potential turtles
                var finishedRow = false;
                for (var x = 0; x < inventory.width(); x++) {
                    var item = inventory.getItem(x, y);
                    if (!item.isEmpty()) {
                        if (finishedRow) {
                            return ItemStack.EMPTY;
                        }

                        if (item.getItem() instanceof TurtleItem) {
                            // Item is a turtle
                            if (turtle.isEmpty()) {
                                turtle = item;
                            } else {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            // Item is not a turtle
                            if (turtle.isEmpty() && leftItem.isEmpty()) {
                                leftItem = item;
                            } else if (!turtle.isEmpty() && rightItem.isEmpty()) {
                                rightItem = item;
                            } else {
                                return ItemStack.EMPTY;
                            }
                        }
                    } else {
                        // Item is empty
                        if (!leftItem.isEmpty() || !turtle.isEmpty()) {
                            finishedRow = true;
                        }
                    }
                }

                // If we found anything, check we found a turtle too
                if (turtle.isEmpty() && (!leftItem.isEmpty() || !rightItem.isEmpty())) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Turtle is already found, just check this row is empty
                for (var x = 0; x < inventory.width(); x++) {
                    var item = inventory.getItem(x, y);
                    if (!item.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // See if we found a turtle + one or more items
        if (turtle.isEmpty() || leftItem.isEmpty() && rightItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // At this point we have a turtle + 1 or 2 items
        // Get the turtle we already have
        @SuppressWarnings({ "unchecked", "rawtypes" })
        UpgradeData<ITurtleUpgrade>[] upgrades = new UpgradeData[]{
            TurtleItem.getUpgradeWithData(turtle, TurtleSide.LEFT),
            TurtleItem.getUpgradeWithData(turtle, TurtleSide.RIGHT),
        };

        // Get the upgrades for the new items.
        // Note: because the turtle is facing towards us, the directions are flipped. Items placed to the left
        // of the turtle, are equipped on its right (and vice versa).
        var items = new ItemStack[]{ rightItem, leftItem };
        for (var i = 0; i < 2; i++) {
            if (!items[i].isEmpty()) {
                var itemUpgrade = TurtleUpgrades.instance().get(upgradeRegistry, items[i]);
                if (itemUpgrade == null || upgrades[i] != null) return ItemStack.EMPTY;
                upgrades[i] = itemUpgrade;
            }
        }

        var newStack = turtle.copyWithCount(1);
        newStack.set(ModRegistry.DataComponents.LEFT_TURTLE_UPGRADE.get(), upgrades[0]);
        newStack.set(ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), upgrades[1]);
        return newStack;
    }

    @Override
    public RecipeSerializer<? extends TurtleUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE_UPGRADE.get();
    }
}
