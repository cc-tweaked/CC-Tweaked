// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Additional {@link StreamCodec}s.
 *
 * @see ByteBufCodecs
 */
public class MoreStreamCodecs {
    public static <B extends FriendlyByteBuf, C extends Enum<C>> StreamCodec<B, C> ofEnum(Class<C> klass) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                return buffer.readEnum(klass);
            }

            @Override
            public void encode(B buffer, C value) {
                buffer.writeEnum(value);
            }
        };
    }

    public static <B extends FriendlyByteBuf, C> StreamCodec<B, NonNullList<C>> nonNullList(StreamCodec<B, C> codec, C empty) {
        return new StreamCodec<>() {
            @Override
            public NonNullList<C> decode(B buffer) {
                var count = buffer.readVarInt();
                var result = NonNullList.withSize(count, empty);
                for (var i = 0; i < result.size(); i++) result.set(i, codec.decode(buffer));
                return result;
            }

            @Override
            public void encode(B buffer, NonNullList<C> list) {
                var count = buffer.writeVarInt(list.size());
                for (var entry : list) codec.encode(buffer, entry);
            }
        };
    }

    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, Vec3::x,
        ByteBufCodecs.DOUBLE, Vec3::y,
        ByteBufCodecs.DOUBLE, Vec3::z,
        Vec3::new
    );

    /**
     * A codec for {@link OptionalInt}. This uses the same wire format as {@link ByteBufCodecs#optional(StreamCodec)}
     * and {@link ByteBufCodecs#VAR_INT}.
     */
    public static final StreamCodec<ByteBuf, OptionalInt> OPTIONAL_INT = new StreamCodec<>() {
        @Override
        public OptionalInt decode(ByteBuf buf) {
            return buf.readBoolean() ? OptionalInt.of(ByteBufCodecs.VAR_INT.decode(buf)) : OptionalInt.empty();
        }

        @Override
        public void encode(ByteBuf buf, OptionalInt optional) {
            if (optional.isPresent()) {
                buf.writeBoolean(true);
                ByteBufCodecs.VAR_INT.encode(buf, optional.getAsInt());
            } else {
                buf.writeBoolean(false);
            }
        }
    };

    /**
     * Equivalent to {@link ByteBufCodecs#BYTE_ARRAY}, but into an immutable {@link ByteBuffer}.
     */
    public static final StreamCodec<ByteBuf, ByteBuffer> BYTE_BUFFER = new StreamCodec<>() {
        @Override
        public ByteBuffer decode(ByteBuf buf) {
            var toRead = VarInt.read(buf);
            if (toRead > buf.readableBytes()) {
                throw new DecoderException("ByteArray with size " + toRead + " is bigger than allowed");
            }

            var bytes = new byte[toRead];
            buf.readBytes(bytes);
            return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
        }

        @Override
        public void encode(ByteBuf buf, ByteBuffer buffer) {
            VarInt.write(buf, buffer.remaining());
            buf.writeBytes(buffer.duplicate());
        }
    };
}
