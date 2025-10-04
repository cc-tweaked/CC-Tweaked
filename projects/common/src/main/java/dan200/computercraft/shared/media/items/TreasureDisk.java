// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/**
 * Stores information about a {@linkplain ModRegistry.Items#TREASURE_DISK treasure disk's} mount.
 *
 * @param name The name/title of the disk.
 * @param path The subpath to the resource
 * @see ModRegistry.DataComponents#TREASURE_DISK
 */
public record TreasureDisk(String name, String path) implements TooltipProvider {
    public static final TreasureDisk UNDEFINED = new TreasureDisk("'missingno' by how did you get this anyway?", "undefined");

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
        return holder.getOrDefault(ModRegistry.DataComponents.TREASURE_DISK.get(), UNDEFINED).name();
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flags, DataComponentGetter components) {
        out.accept(Component.literal(name()));
    }
}
