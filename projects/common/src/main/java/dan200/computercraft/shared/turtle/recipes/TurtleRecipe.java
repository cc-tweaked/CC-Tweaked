// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.recipes;

import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerConvertRecipe;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * The recipe which crafts a turtle from an existing computer item.
 */
public final class TurtleRecipe extends ComputerConvertRecipe {
    private final TurtleItem turtle;

    private TurtleRecipe(ResourceLocation id, ShapedRecipeSpec recipe, TurtleItem turtle) {
        super(id, recipe);
        this.turtle = turtle;
    }

    public static DataResult<TurtleRecipe> of(ResourceLocation id, ShapedRecipeSpec recipe) {
        if (!(recipe.result().getItem() instanceof TurtleItem turtle)) {
            return DataResult.error(() -> recipe.result().getItem() + " is not a turtle item");
        }

        return DataResult.success(new TurtleRecipe(id, recipe, turtle));
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        var computerID = item.getComputerID(stack);
        var label = item.getLabel(stack);

        return turtle.create(computerID, label, -1, null, null, 0, null);
    }

    @Override
    public RecipeSerializer<TurtleRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE.get();
    }
}
