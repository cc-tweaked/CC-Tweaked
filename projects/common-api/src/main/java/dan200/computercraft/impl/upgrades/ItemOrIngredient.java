// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

/**
 * Codec for an {@link Ingredient} which writes ingredients representing a single item as a plain string.
 */
public final class ItemOrIngredient {
    private ItemOrIngredient() {
    }

    public static final Codec<Ingredient> CODEC = Codec.either(ItemStack.SIMPLE_ITEM_CODEC, Ingredient.CODEC_NONEMPTY)
        .xmap(e -> e.map(Ingredient::of, x -> x), ItemOrIngredient::wrap);

    private static Either<ItemStack, Ingredient> wrap(Ingredient ingredient) {
        var encoded = Ingredient.CODEC_NONEMPTY.encodeStart(NbtOps.INSTANCE, ingredient).getOrThrow();

        if (encoded instanceof CompoundTag compound && compound.contains("item", CompoundTag.TAG_STRING)) {
            var item = ItemStack.SIMPLE_ITEM_CODEC.parse(NbtOps.INSTANCE, Objects.requireNonNull(compound.get("item"))).result();
            if (item.isPresent()) return Either.left(item.get());
        }

        return Either.right(ingredient);
    }
}
