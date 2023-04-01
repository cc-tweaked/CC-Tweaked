// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PrintoutRecipe extends CustomRecipe {
    private final Ingredient leather;
    private final Ingredient string;

    public PrintoutRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);

        var ingredients = PlatformHelper.get().getRecipeIngredients();
        leather = ingredients.leather();
        string = ingredients.string();
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 3 && y >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return PrintoutItem.createMultipleFromTitleAndText(null, null, null);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        return !assemble(inventory, world.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
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
        if (numPages <= PrintoutItem.MAX_PAGES && stringFound && printoutFound && numPrintouts >= (leatherFound ? 1 : 2)) {
            if (printouts == null) throw new IllegalStateException("Printouts must be non-null");
            var text = new String[numPages * PrintoutItem.LINES_PER_PAGE];
            var colours = new String[numPages * PrintoutItem.LINES_PER_PAGE];
            var line = 0;

            for (var printout = 0; printout < numPrintouts; printout++) {
                var stack = printouts[printout];
                if (stack.getItem() instanceof PrintoutItem) {
                    // Add a printout
                    var pageText = PrintoutItem.getText(printouts[printout]);
                    var pageColours = PrintoutItem.getColours(printouts[printout]);
                    for (var pageLine = 0; pageLine < pageText.length; pageLine++) {
                        text[line] = pageText[pageLine];
                        colours[line] = pageColours[pageLine];
                        line++;
                    }
                } else {
                    // Add a blank page
                    for (var pageLine = 0; pageLine < PrintoutItem.LINES_PER_PAGE; pageLine++) {
                        text[line] = "";
                        colours[line] = "";
                        line++;
                    }
                }
            }

            String title = null;
            if (printouts[0].getItem() instanceof PrintoutItem) {
                title = PrintoutItem.getTitle(printouts[0]);
            }

            if (leatherFound) {
                return PrintoutItem.createBookFromTitleAndText(title, text, colours);
            } else {
                return PrintoutItem.createMultipleFromTitleAndText(title, text, colours);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.PRINTOUT.get();
    }
}
