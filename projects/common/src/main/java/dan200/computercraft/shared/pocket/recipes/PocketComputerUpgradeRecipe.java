// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.recipes;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.core.PocketSide;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PocketComputerUpgradeRecipe extends CustomRecipe {
    private final HolderLookup<IPocketUpgrade> upgradeRegistry;

    public PocketComputerUpgradeRecipe(HolderLookup<IPocketUpgrade> upgradeRegistry) {
        this.upgradeRegistry = upgradeRegistry;
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        return !assemble(inventory).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        // Scan the grid for a pocket computer
        var computer = ItemStack.EMPTY;
        var computerX = -1;
        var computerY = -1;
        computer:
        for (var y = 0; y < inventory.height(); y++) {
            for (var x = 0; x < inventory.width(); x++) {
                var item = inventory.getItem(x, y);
                if (!item.isEmpty() && item.getItem() instanceof PocketComputerItem) {
                    computer = item;
                    computerX = x;
                    computerY = y;
                    break computer;
                }
            }
        }

        if (computer.isEmpty()) return ItemStack.EMPTY;

        // Check for upgrades around the item
        UpgradeData<IPocketUpgrade> above = null, below = null;
        for (var y = 0; y < inventory.height(); y++) {
            for (var x = 0; x < inventory.width(); x++) {
                var item = inventory.getItem(x, y);
                if (item.isEmpty() || (x == computerX && y == computerY)) continue;

                if (x == computerX && y == computerY - 1) {
                    above = PocketUpgrades.instance().get(upgradeRegistry, item);
                    if (above == null) return ItemStack.EMPTY;
                } else if (x == computerX && y == computerY + 1) {
                    below = PocketUpgrades.instance().get(upgradeRegistry, item);
                    if (below == null) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        // Abort if we have no upgrades
        if (above == null && below == null) return ItemStack.EMPTY;
        // Or if we've already got an upgrade in that slot.
        if ((above != null && PocketComputerItem.getUpgrade(computer, PocketSide.BACK) != null)
            || (below != null && PocketComputerItem.getUpgrade(computer, PocketSide.BOTTOM) != null)) {
            return ItemStack.EMPTY;
        }

        // Construct the new stack
        var result = computer.copyWithCount(1);
        if (above != null) result.set(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), above);
        if (below != null) result.set(ModRegistry.DataComponents.BOTTOM_POCKET_UPGRADE.get(), below);
        return result;
    }

    @Override
    public RecipeSerializer<? extends PocketComputerUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.POCKET_COMPUTER_UPGRADE.get();
    }
}
