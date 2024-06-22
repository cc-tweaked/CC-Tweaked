// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.recipe.RecipeProperties;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

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
public final class PrintoutRecipe extends ShapelessRecipe {
    public static final MapCodec<PrintoutRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShapelessRecipeSpec.CODEC.forGetter(PrintoutRecipe::toSpec),
        Ingredient.CODEC_NONEMPTY.fieldOf("printout").forGetter(x -> x.printout),
        ExtraCodecs.POSITIVE_INT.fieldOf("min_printouts").forGetter(x -> x.minPrintouts)
    ).apply(instance, PrintoutRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrintoutRecipe> STREAM_CODEC = StreamCodec.composite(
        ShapelessRecipeSpec.STREAM_CODEC, PrintoutRecipe::toSpec,
        Ingredient.CONTENTS_STREAM_CODEC, x -> x.printout,
        ByteBufCodecs.VAR_INT, x -> x.minPrintouts,
        PrintoutRecipe::new
    );

    private final NonNullList<Ingredient> ingredients;
    private final Ingredient printout;
    private final int minPrintouts;
    private final ShapelessRecipe innerRecipe;

    private final ItemStack result;

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
        // We use the full list of ingredients in the recipe itself, so that it behaves sensibly with recipe mods.
        super(spec.properties().group(), spec.properties().category(), spec.result(), concat(spec.ingredients(), printout, minPrintouts));

        this.ingredients = spec.ingredients();
        this.printout = printout;
        this.minPrintouts = minPrintouts;
        this.result = spec.result();

        // However, when testing whether the recipe matches, we only want to use the non-printout ingredients. To do
        // that, we create a hidden recipe with the main ingredients.
        this.innerRecipe = spec.create();
    }

    private static NonNullList<Ingredient> concat(NonNullList<Ingredient> first, Ingredient pages, int pagesRequired) {
        var result = NonNullList.withSize(first.size() + pagesRequired, Ingredient.EMPTY);
        var idx = 0;
        for (var ingredient : first) result.set(idx++, ingredient);
        for (var i = 0; i < pagesRequired; i++) result.set(idx++, pages);
        return result;
    }

    private ShapelessRecipeSpec toSpec() {
        return new ShapelessRecipeSpec(RecipeProperties.of(this), ingredients, result);
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var stackedContents = new StackedContents();

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
            && inputs == ingredients.size() && stackedContents.canCraft(innerRecipe, null);
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

        var result = super.assemble(inv, registries);
        result.set(ModRegistry.DataComponents.PRINTOUT.get(), new PrintoutData(data.getFirst().title(), lines));
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.PRINTOUT.get();
    }
}
