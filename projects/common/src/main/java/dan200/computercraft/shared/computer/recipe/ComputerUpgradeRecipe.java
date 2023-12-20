// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.recipe;

import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * A recipe which "upgrades" a {@linkplain IComputerItem computer}, converting to it a new item (for instance a normal
 * turtle to an advanced one).
 *
 * @see IComputerItem#changeItem(ItemStack, Item)
 */
public final class ComputerUpgradeRecipe extends ComputerConvertRecipe {
    private final Item result;

    private ComputerUpgradeRecipe(ResourceLocation identifier, ShapedRecipeSpec recipe) {
        super(identifier, recipe);
        this.result = recipe.result().getItem();
    }

    public static DataResult<ComputerUpgradeRecipe> of(ResourceLocation id, ShapedRecipeSpec recipe) {
        if (!(recipe.result().getItem() instanceof IComputerItem)) {
            return DataResult.error(() -> recipe.result().getItem() + " is not a computer item");
        }

        return DataResult.success(new ComputerUpgradeRecipe(id, recipe));
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        return item.changeItem(stack, result);
    }

    @Override
    public RecipeSerializer<ComputerUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get();
    }
}
