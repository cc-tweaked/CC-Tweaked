// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A reference to a {@link ServerComputer}.
 *
 * @param session  The current {@linkplain ServerComputerRegistry#getSessionID() session id}.
 * @param instance The computer's {@linkplain ServerComputer#getInstanceUUID() instance id}.
 */
public record ServerComputerReference(int session, UUID instance) {
    public static final Codec<ServerComputerReference> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("session").forGetter(ServerComputerReference::session),
        UUIDUtil.CODEC.fieldOf("instance").forGetter(ServerComputerReference::instance)
    ).apply(i, ServerComputerReference::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerComputerReference> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ServerComputerReference::session,
        UUIDUtil.STREAM_CODEC, ServerComputerReference::instance,
        ServerComputerReference::new
    );

    public @Nullable ServerComputer get(ServerComputerRegistry registry) {
        return registry.get(session, this.instance());
    }

    public static @Nullable ServerComputer get(DataComponentHolder holder, ServerComputerRegistry registry) {
        var reference = holder.get(ModRegistry.DataComponents.COMPUTER.get());
        return reference == null ? null : reference.get(registry);
    }
}
