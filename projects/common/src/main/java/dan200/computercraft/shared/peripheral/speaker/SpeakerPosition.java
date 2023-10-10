// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import net.minecraft.network.FriendlyByteBuf;
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
        public static Message read(FriendlyByteBuf buffer) {
            var level = buffer.readResourceLocation();
            var position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            var entity = buffer.readBoolean() ? OptionalInt.of(buffer.readInt()) : OptionalInt.empty();
            return new Message(level, position, entity);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(level);

            buffer.writeDouble(position.x);
            buffer.writeDouble(position.y);
            buffer.writeDouble(position.z);

            buffer.writeBoolean(entity.isPresent());
            if (entity.isPresent()) buffer.writeInt(entity.getAsInt());
        }
    }
}
