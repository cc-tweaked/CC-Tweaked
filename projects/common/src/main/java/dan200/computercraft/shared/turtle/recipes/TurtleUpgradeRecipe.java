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
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class TurtleUpgradeRecipe extends CustomRecipe {
    public TurtleUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 3 && y >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ModRegistry.Items.TURTLE_NORMAL.get().create(-1, null, -1, null, null, 0, null);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        return !assemble(inventory, world.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        // Scan the grid for a row containing a turtle and 1 or 2 items
        var leftItem = ItemStack.EMPTY;
        var turtle = ItemStack.EMPTY;
        var rightItem = ItemStack.EMPTY;

        for (var y = 0; y < inventory.getHeight(); y++) {
            if (turtle.isEmpty()) {
                // Search this row for potential turtles
                var finishedRow = false;
                for (var x = 0; x < inventory.getWidth(); x++) {
                    var item = inventory.getItem(x + y * inventory.getWidth());
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
                for (var x = 0; x < inventory.getWidth(); x++) {
                    var item = inventory.getItem(x + y * inventory.getWidth());
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
        var itemTurtle = (TurtleItem) turtle.getItem();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        UpgradeData<ITurtleUpgrade>[] upgrades = new UpgradeData[]{
            itemTurtle.getUpgradeWithData(turtle, TurtleSide.LEFT),
            itemTurtle.getUpgradeWithData(turtle, TurtleSide.RIGHT),
        };

        // Get the upgrades for the new items
        var items = new ItemStack[]{ rightItem, leftItem };
        for (var i = 0; i < 2; i++) {
            if (!items[i].isEmpty()) {
                var itemUpgrade = TurtleUpgrades.instance().get(items[i]);
                if (itemUpgrade == null || upgrades[i] != null) return ItemStack.EMPTY;
                upgrades[i] = itemUpgrade;
            }
        }

        // Construct the new stack
        var computerID = itemTurtle.getComputerID(turtle);
        var label = itemTurtle.getLabel(turtle);
        var fuelLevel = itemTurtle.getFuelLevel(turtle);
        var colour = itemTurtle.getColour(turtle);
        var overlay = itemTurtle.getOverlay(turtle);
        return itemTurtle.create(computerID, label, colour, upgrades[0], upgrades[1], fuelLevel, overlay);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE_UPGRADE.get();
    }
}
