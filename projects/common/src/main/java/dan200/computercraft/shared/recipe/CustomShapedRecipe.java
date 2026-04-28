// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A custom version of {@link ShapedRecipe}, which can be converted to and from a {@link ShapedRecipeSpec}.
 */
public abstract class CustomShapedRecipe extends NormalCraftingRecipe {
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public CustomShapedRecipe(ShapedRecipeSpec recipe) {
        super(recipe.properties().common(), recipe.properties().book());
        this.pattern = recipe.pattern();
        this.result = recipe.result();
    }

    public final ShapedRecipeSpec toSpec() {
        return new ShapedRecipeSpec(RecipeProperties.of(this), pattern, result);
    }

    @Override
    protected final PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(pattern.ingredients());
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        return result.create();
    }

    @Override
    public final List<RecipeDisplay> display() {
        return List.of(new ShapedCraftingRecipeDisplay(
            pattern.width(), pattern.height(),
            pattern.ingredients().stream().map(e -> e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
            new SlotDisplay.ItemStackSlotDisplay(result),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    @Override
    public abstract RecipeSerializer<? extends CustomShapedRecipe> getSerializer();
}
