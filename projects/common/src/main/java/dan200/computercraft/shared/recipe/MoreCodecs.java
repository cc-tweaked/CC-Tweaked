// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/**
 * Additional codecs for working with recipes.
 */
public class MoreCodecs {
    /**
     * A codec for {@link CompoundTag}s, which either accepts a NBT-string or a JSON object.
     */
    public static final Codec<CompoundTag> TAG = Codec.either(Codec.STRING, CompoundTag.CODEC).flatXmap(
        either -> either.map(MoreCodecs::parseTag, DataResult::success),
        nbtCompound -> DataResult.success(Either.left(nbtCompound.getAsString()))
    );

    private static DataResult<CompoundTag> parseTag(String contents) {
        try {
            return DataResult.success(TagParser.parseTag(contents));
        } catch (CommandSyntaxException e) {
            return DataResult.error(e::getMessage);
        }
    }
}
