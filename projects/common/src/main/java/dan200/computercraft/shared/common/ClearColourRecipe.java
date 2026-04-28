// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.common;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftTags;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Craft a wet sponge with a {@linkplain ComputerCraftTags.Items#DYEABLE dyable item} to remove its dye.
 */
public final class ClearColourRecipe extends CustomRecipe {
    public static final ClearColourRecipe INSTANCE = new ClearColourRecipe();
    public static final MapCodec<ClearColourRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearColourRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ClearColourRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private ClearColourRecipe() {
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var hasColourable = false;
        var hasSponge = false;
        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ComputerCraftTags.Items.DYEABLE)) {
                if (hasColourable) return false;
                if (!stack.has(DataComponents.DYED_COLOR)) return false;
                hasColourable = true;
            } else if (stack.getItem() == Items.WET_SPONGE) {
                if (hasSponge) return false;
                hasSponge = true;
            } else {
                return false;
            }
        }

        return hasColourable && hasSponge;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        var colourable = ItemStack.EMPTY;

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);
            if (stack.is(ComputerCraftTags.Items.DYEABLE)) colourable = stack;
        }

        if (colourable.isEmpty()) return ItemStack.EMPTY;

        var result = colourable.copyWithCount(1);
        result.remove(DataComponents.DYED_COLOR);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        var remaining = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (var i = 0; i < remaining.size(); i++) {
            if (container.getItem(i).getItem() == Items.WET_SPONGE) remaining.set(i, new ItemStack(Items.WET_SPONGE));
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<ClearColourRecipe> getSerializer() {
        return SERIALIZER;
    }
}
