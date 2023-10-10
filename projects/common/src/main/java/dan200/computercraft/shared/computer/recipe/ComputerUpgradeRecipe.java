// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.recipe;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * A recipe which "upgrades" a {@linkplain IComputerItem computer}, converting it from one {@linkplain ComputerFamily
 * family} to another.
 */
public final class ComputerUpgradeRecipe extends ComputerConvertRecipe {
    private final ComputerFamily family;

    private ComputerUpgradeRecipe(ResourceLocation identifier, ShapedRecipeSpec recipe, ComputerFamily family) {
        super(identifier, recipe);
        this.family = family;
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        return item.withFamily(stack, family);
    }

    @Override
    public RecipeSerializer<ComputerUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get();
    }

    public static class Serializer implements RecipeSerializer<ComputerUpgradeRecipe> {
        @Override
        public ComputerUpgradeRecipe fromJson(ResourceLocation identifier, JsonObject json) {
            var recipe = ShapedRecipeSpec.fromJson(json);
            var family = ComputerFamily.getFamily(json, "family");
            return new ComputerUpgradeRecipe(identifier, recipe, family);
        }

        @Override
        public ComputerUpgradeRecipe fromNetwork(ResourceLocation identifier, FriendlyByteBuf buf) {
            var recipe = ShapedRecipeSpec.fromNetwork(buf);
            var family = buf.readEnum(ComputerFamily.class);
            return new ComputerUpgradeRecipe(identifier, recipe, family);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ComputerUpgradeRecipe recipe) {
            recipe.toSpec().toNetwork(buf);
            buf.writeEnum(recipe.family);
        }
    }
}
