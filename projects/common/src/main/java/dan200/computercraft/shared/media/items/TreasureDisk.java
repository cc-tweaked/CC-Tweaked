// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stores information about a {@linkplain TreasureDiskItem treasure disk's} mount.
 *
 * @param name The name/title of the disk.
 * @param path The subpath to the resource
 * @see ModRegistry.DataComponents#TREASURE_DISK
 */
public record TreasureDisk(String name, String path) {
    public static final Codec<TreasureDisk> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(TreasureDisk::name),
        Codec.STRING.fieldOf("path").forGetter(TreasureDisk::path)
    ).apply(i, TreasureDisk::new));

    public static final StreamCodec<FriendlyByteBuf, TreasureDisk> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TreasureDisk::name,
        ByteBufCodecs.STRING_UTF8, TreasureDisk::path,
        TreasureDisk::new
    );

    public static String getTitle(DataComponentHolder holder) {
        var nbt = holder.get(ModRegistry.DataComponents.TREASURE_DISK.get());
        return nbt != null ? nbt.name() : "'missingno' by how did you get this anyway?";
    }
}
