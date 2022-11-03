/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;

public record SpeakerPosition(@Nullable Level level, @Nonnull Vec3 position, @Nullable Entity entity) {
    public static SpeakerPosition of(@Nullable Level level, @Nonnull Vec3 position) {
        return new SpeakerPosition(level, position, null);
    }

    public static SpeakerPosition of(@Nonnull Entity entity) {
        return new SpeakerPosition(entity.level, entity.getEyePosition(1), entity);
    }

    public boolean withinDistance(SpeakerPosition other, double distanceSq) {
        return level == other.level && entity == other.entity && position.distanceToSqr(other.position) <= distanceSq;
    }

    public Message asMessage() {
        if (level == null) throw new NullPointerException("Cannot send a position without a level");
        return new Message(level.dimension().location(), position, entity == null ? OptionalInt.empty() : OptionalInt.of(entity.getId()));
    }

    public static final class Message {
        private final ResourceLocation level;
        private final Vec3 position;
        private final OptionalInt entity;

        private Message(ResourceLocation level, Vec3 position, OptionalInt entity) {
            this.level = level;
            this.position = position;
            this.entity = entity;
        }

        public static Message read(@Nonnull FriendlyByteBuf buffer) {
            var level = buffer.readResourceLocation();
            var position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            var entity = buffer.readBoolean() ? OptionalInt.of(buffer.readInt()) : OptionalInt.empty();
            return new Message(level, position, entity);
        }

        public void write(@Nonnull FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(level);

            buffer.writeDouble(position.x);
            buffer.writeDouble(position.y);
            buffer.writeDouble(position.z);

            buffer.writeBoolean(entity.isPresent());
            if (entity.isPresent()) buffer.writeInt(entity.getAsInt());
        }

        @Nonnull
        @OnlyIn(Dist.CLIENT)
        public SpeakerPosition reify() {
            var minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            if (level != null && !level.dimension().location().equals(this.level)) level = null;

            return new SpeakerPosition(
                level, position,
                level != null && entity.isPresent() ? level.getEntity(entity.getAsInt()) : null
            );
        }
    }
}
