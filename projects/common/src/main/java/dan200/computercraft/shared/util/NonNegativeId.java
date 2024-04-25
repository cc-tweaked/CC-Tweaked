// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.ComputerCraftAPI;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * A non-negative integer id, used for computer and disk ids.
 *
 * @param id The id of this entity.
 * @see dan200.computercraft.shared.ModRegistry.DataComponents#COMPUTER_ID
 * @see dan200.computercraft.shared.ModRegistry.DataComponents#DISK_ID
 */
public record NonNegativeId(int id) {
    public static final Codec<NonNegativeId> CODEC = ExtraCodecs.NON_NEGATIVE_INT.xmap(NonNegativeId::new, NonNegativeId::id);

    public static final StreamCodec<ByteBuf, NonNegativeId> STREAM_CODEC = ByteBufCodecs.INT.map(NonNegativeId::new, NonNegativeId::id);

    public NonNegativeId {
        if (id < 0) throw new IllegalArgumentException("ID must be >= 0");
    }

    public static int getId(@Nullable NonNegativeId id) {
        return id == null ? -1 : id.id();
    }

    public static @Nullable NonNegativeId of(int id) {
        return id >= 0 ? new NonNegativeId(id) : null;
    }

    public static int getOrCreate(MinecraftServer server, ItemStack stack, DataComponentType<NonNegativeId> component, String type) {
        var id = stack.get(component);
        if (id != null) return id.id();

        var diskID = ComputerCraftAPI.createUniqueNumberedSaveDir(server, type);
        stack.set(component, new NonNegativeId(diskID));
        return diskID;
    }
}
