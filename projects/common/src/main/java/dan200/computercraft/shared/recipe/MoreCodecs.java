// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;


/**
 * Additional codecs for working with recipes.
 */
public class MoreCodecs {
    /**
     * A non-air item.
     */
    private static final Codec<Item> ITEM_NOT_AIR = ExtraCodecs.validate(
        BuiltInRegistries.ITEM.byNameCodec(),
        item -> item == Items.AIR ? DataResult.error(() -> "Crafting result must not be minecraft:air") : DataResult.success(item)
    );

    /**
     * A codec for {@link CompoundTag}s, which either accepts a NBT-string or a JSON object.
     */
    public static final Codec<CompoundTag> TAG = Codec.either(Codec.STRING, CompoundTag.CODEC).flatXmap(
        either -> either.map(MoreCodecs::parseTag, DataResult::success),
        nbtCompound -> DataResult.success(Either.left(nbtCompound.getAsString()))
    );

    /**
     * A {@link ItemStack}, similar to {@link ItemStack#ITEM_WITH_COUNT_CODEC} or {@link ItemStack#CODEC},
     * but using {@link #TAG} to parse the stack's NBT.
     */
    public static final Codec<ItemStack> ITEM_STACK_WITH_NBT = RecordCodecBuilder.create(instance -> instance.group(
        ITEM_NOT_AIR.fieldOf("item").forGetter(ItemStack::getItem),
        ExtraCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
        ExtraCodecs.strictOptionalField(TAG, "nbt").forGetter(x -> Optional.ofNullable(x.getTag()))
    ).apply(instance, MoreCodecs::createTag));

    /**
     * A list of {@link Ingredient}s, usable in a {@linkplain ShapelessRecipe shapeless recipe}.
     */
    public static final Codec<NonNullList<Ingredient>> SHAPELESS_INGREDIENTS = Ingredient.CODEC_NONEMPTY.listOf().flatXmap(list -> {
        var ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
        if (ingredients.length == 0) return DataResult.error(() -> "No ingredients for shapeless recipe");
        if (ingredients.length > 9) return DataResult.error(() -> "Too many ingredients for shapeless recipe");
        return DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
    }, DataResult::success);

    private static DataResult<CompoundTag> parseTag(String contents) {
        try {
            return DataResult.success(TagParser.parseTag(contents));
        } catch (CommandSyntaxException e) {
            return DataResult.error(e::getMessage);
        }
    }

    private static ItemStack createTag(ItemLike item, int count, Optional<CompoundTag> tag) {
        var stack = new ItemStack(item, count);
        tag.ifPresent(stack::setTag);
        return stack;
    }
}
