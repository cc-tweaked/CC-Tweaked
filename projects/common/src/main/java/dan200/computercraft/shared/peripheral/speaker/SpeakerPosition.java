// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.OptionalInt;

public record SpeakerPosition(@Nullable Level level, Vec3 position, @Nullable Entity entity) {
    public static SpeakerPosition of(@Nullable Level level, Vec3 position) {
        return new SpeakerPosition(level, position, null);
    }

    public static SpeakerPosition of(Entity entity) {
        return new SpeakerPosition(entity.level(), entity.getEyePosition(1), entity);
    }

    public boolean withinDistance(SpeakerPosition other, double distanceSq) {
        return level == other.level && entity == other.entity && position.distanceToSqr(other.position) <= distanceSq;
    }

    public Message asMessage() {
        if (level == null) throw new NullPointerException("Cannot send a position without a level");
        return new Message(level.dimension().location(), position, entity == null ? OptionalInt.empty() : OptionalInt.of(entity.getId()));
    }

    public record Message(
        ResourceLocation level,
        Vec3 position,
        OptionalInt entity
    ) {
        public static final StreamCodec<FriendlyByteBuf, Message> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Message::level,
            MoreStreamCodecs.VEC3, Message::position,
            MoreStreamCodecs.OPTIONAL_INT, Message::entity,
            Message::new
        );
    }
}
