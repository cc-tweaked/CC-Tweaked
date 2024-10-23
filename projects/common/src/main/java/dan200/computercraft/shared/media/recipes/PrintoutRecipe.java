// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.recipe.AbstractCraftingRecipe;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe for combining one or more printed pages together.
 * <p>
 * This behaves similarly to a {@link ShapelessRecipe}, but allows a variable number of pages to appear as ingredients.
 *
 * @see PrintoutItem
 * @see PrintoutData
 */
public final class PrintoutRecipe extends AbstractCraftingRecipe {
    public static final MapCodec<PrintoutRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShapelessRecipeSpec.CODEC.forGetter(PrintoutRecipe::toSpec),
        Ingredient.CODEC.fieldOf("printout").forGetter(x -> x.printout),
        ExtraCodecs.POSITIVE_INT.fieldOf("min_printouts").forGetter(x -> x.minPrintouts)
    ).apply(instance, PrintoutRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrintoutRecipe> STREAM_CODEC = StreamCodec.composite(
        ShapelessRecipeSpec.STREAM_CODEC, PrintoutRecipe::toSpec,
        Ingredient.CONTENTS_STREAM_CODEC, x -> x.printout,
        ByteBufCodecs.VAR_INT, x -> x.minPrintouts,
        PrintoutRecipe::new
    );

    private final ShapelessRecipeSpec spec;
    private final List<Ingredient> ingredients;
    private @Nullable PlacementInfo ingredientInfo;

    private final List<Ingredient> placementIngredients;
    private @Nullable PlacementInfo placementInfo;

    private final Ingredient printout;
    private final int minPrintouts;

    /**
     * Construct a new {@link PrintoutRecipe}.
     *
     * @param spec         The base {@link ShapelessRecipeSpec} for this recipe.
     * @param printout     The items that will be treated as printed pages.
     * @param minPrintouts The minimum number of pages required.
     */
    public PrintoutRecipe(
        ShapelessRecipeSpec spec, Ingredient printout, int minPrintouts
    ) {
        super(spec.properties());

        this.spec = spec;
        this.ingredients = spec.ingredients();
        // We use the full list of ingredients for the display/placement information.
        this.placementIngredients = concat(spec.ingredients(), printout, minPrintouts);

        this.printout = printout;
        this.minPrintouts = minPrintouts;
    }

    private static List<Ingredient> concat(List<Ingredient> first, Ingredient pages, int pagesRequired) {
        var result = new ArrayList<Ingredient>(first.size() + pagesRequired);
        result.addAll(first);
        for (var i = 0; i < pagesRequired; i++) result.add(pages);
        return result;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(placementIngredients);
        return placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapelessCraftingRecipeDisplay(
                placementIngredients.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(spec.result()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var stackedContents = new StackedItemContents();

        var inputs = 0;
        var printouts = 0;
        var pages = 0;
        var hasPrintout = false;

        for (var j = 0; j < inv.size(); ++j) {
            var stack = inv.getItem(j);
            if (stack.isEmpty()) continue;
            if (printout.test(stack)) {
                printouts++;

                var printout = stack.get(ModRegistry.DataComponents.PRINTOUT.get());
                if (printout == null) {
                    pages++;
                } else {
                    hasPrintout = true;
                    pages += printout.pages();
                }
            } else {
                inputs++;
                stackedContents.accountStack(stack, 1);
            }
        }

        return hasPrintout && printouts >= minPrintouts && pages <= PrintoutData.MAX_PAGES
            && inputs == ingredients.size() && stackedContents.canCraft(getIngredientInfo().unpackedIngredients(), null);
    }

    private PlacementInfo getIngredientInfo() {
        // However, when testing whether the recipe matches, we only want to use the non-printout ingredients. To do
        // that, we create a hidden recipe with the main ingredients.
        if (ingredientInfo == null) ingredientInfo = PlacementInfo.create(ingredients);
        return ingredientInfo;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        List<PrintoutData> data = new ArrayList<>();
        for (var j = 0; j < inv.size(); ++j) {
            var stack = inv.getItem(j);
            if (!stack.isEmpty() && printout.test(stack)) data.add(PrintoutData.getOrEmpty(stack));
        }

        if (data.isEmpty()) throw new IllegalStateException("Printouts must be non-null");

        var lines = data.stream().flatMap(x -> x.lines().stream()).toList();

        var result = spec.result().copy();
        result.set(ModRegistry.DataComponents.PRINTOUT.get(), new PrintoutData(data.getFirst().title(), lines));
        return result;
    }

    private ShapelessRecipeSpec toSpec() {
        return spec;
    }

    @Override
    public RecipeSerializer<PrintoutRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.PRINTOUT.get();
    }
}
