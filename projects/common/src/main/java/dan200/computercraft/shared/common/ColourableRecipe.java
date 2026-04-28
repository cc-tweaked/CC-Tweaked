// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ColourableRecipe extends CustomRecipe {
    public static final ColourableRecipe INSTANCE = new ColourableRecipe();
    public static final MapCodec<ColourableRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ColourableRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ColourableRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private ColourableRecipe() {
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var hasColourable = false;
        var hasDye = false;
        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
                if (hasColourable) return false;
                hasColourable = true;
            } else if (ColourUtils.isDye(stack)) {
                hasDye = true;
            } else {
                return false;
            }
        }

        return hasColourable && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        var colourable = ItemStack.EMPTY;

        var tracker = new ColourTracker();

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
                colourable = stack;
            } else {
                var dye = ColourUtils.getDyeColour(stack);
                if (dye != null) tracker.addColour(dye);
            }
        }

        return colourable.isEmpty()
            ? ItemStack.EMPTY
            : DataComponentUtil.setDyeColour(colourable.copyWithCount(1), tracker.getColour());
    }

    @Override
    public RecipeSerializer<ColourableRecipe> getSerializer() {
        return SERIALIZER;
    }
}
