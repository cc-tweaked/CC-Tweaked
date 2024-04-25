// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public final class PrintoutRecipe extends CustomRecipe {
    private final Ingredient leather;
    private final Ingredient string;

    public PrintoutRecipe(CraftingBookCategory category) {
        super(category);

        var ingredients = PlatformHelper.get().getRecipeIngredients();
        leather = ingredients.leather();
        string = ingredients.string();
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 3 && y >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return new ItemStack(ModRegistry.Items.PRINTED_PAGES.get());
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        return !assemble(inventory, world.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
        // See if we match the recipe, and extract the input disk ID and dye colour
        var numPages = 0;
        var numPrintouts = 0;
        ItemStack[] printouts = null;
        var stringFound = false;
        var leatherFound = false;
        var printoutFound = false;
        for (var y = 0; y < inventory.getHeight(); y++) {
            for (var x = 0; x < inventory.getWidth(); x++) {
                var stack = inventory.getItem(x + y * inventory.getWidth());
                if (!stack.isEmpty()) {
                    if (stack.getItem() instanceof PrintoutItem printout && printout.getType() != PrintoutItem.Type.BOOK) {
                        if (printouts == null) printouts = new ItemStack[9];
                        printouts[numPrintouts] = stack;
                        numPages += PrintoutItem.getPageCount(stack);
                        numPrintouts++;
                        printoutFound = true;
                    } else if (stack.getItem() == Items.PAPER) {
                        if (printouts == null) {
                            printouts = new ItemStack[9];
                        }
                        printouts[numPrintouts] = stack;
                        numPages++;
                        numPrintouts++;
                    } else if (string.test(stack) && !stringFound) {
                        stringFound = true;
                    } else if (leather.test(stack) && !leatherFound) {
                        leatherFound = true;
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Build some pages with what was passed in
        if (numPages <= PrintoutData.MAX_PAGES && stringFound && printoutFound && numPrintouts >= (leatherFound ? 1 : 2)) {
            if (printouts == null) throw new IllegalStateException("Printouts must be non-null");
            var lines = new PrintoutData.Line[numPages * PrintoutData.LINES_PER_PAGE];
            var line = 0;

            for (var printout = 0; printout < numPrintouts; printout++) {
                var pageText = printouts[printout].get(ModRegistry.DataComponents.PRINTOUT.get());
                if (pageText != null) {
                    // Add a printout
                    for (var pageLine : pageText.lines()) lines[line++] = pageLine;
                } else {
                    // Add a blank page
                    for (var pageLine = 0; pageLine < PrintoutData.LINES_PER_PAGE; pageLine++) {
                        lines[line++] = PrintoutData.Line.EMPTY;
                    }
                }
            }

            var title = PrintoutItem.getTitle(printouts[0]);

            return DataComponentUtil.createStack(
                leatherFound ? ModRegistry.Items.PRINTED_BOOK.get() : ModRegistry.Items.PRINTED_PAGES.get(),
                ModRegistry.DataComponents.PRINTOUT.get(), new PrintoutData(title, List.of(lines))
            );
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.PRINTOUT.get();
    }
}
