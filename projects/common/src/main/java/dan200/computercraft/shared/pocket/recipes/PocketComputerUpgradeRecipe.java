// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.recipes;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PocketComputerUpgradeRecipe extends CustomRecipe {
    public PocketComputerUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        return !assemble(inventory, world.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
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

        if (PocketComputerItem.getUpgradeWithData(computer) != null) return ItemStack.EMPTY;

        // Check for upgrades around the item
        UpgradeData<IPocketUpgrade> upgrade = null;
        for (var y = 0; y < inventory.height(); y++) {
            for (var x = 0; x < inventory.width(); x++) {
                var item = inventory.getItem(x, y);
                if (x == computerX && y == computerY) continue;

                if (x == computerX && y == computerY - 1) {
                    upgrade = PocketUpgrades.instance().get(registryAccess, item);
                    if (upgrade == null) return ItemStack.EMPTY;
                } else if (!item.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (upgrade == null) return ItemStack.EMPTY;

        // Construct the new stack
        var result = computer.copyWithCount(1);
        result.set(ModRegistry.DataComponents.POCKET_UPGRADE.get(), upgrade);
        return result;
    }

    @Override
    public RecipeSerializer<? extends PocketComputerUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.POCKET_COMPUTER_UPGRADE.get();
    }
}
