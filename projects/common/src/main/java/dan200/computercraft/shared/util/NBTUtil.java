// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.mojang.serialization.Codec;
import dan200.computercraft.core.util.Nullability;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class NBTUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NBTUtil.class);
    @VisibleForTesting
    static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();

    private NBTUtil() {
    }

    public static <T> @Nullable T decodeFrom(Codec<T> codec, HolderLookup.Provider registries, CompoundTag tag, String key) {
        var childTag = tag.get(key);
        return childTag == null ? null : codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), childTag)
            .resultOrPartial(e -> LOG.warn("Failed to parse NBT: {}", e))
            .orElse(null);
    }

    public static <T> void encodeTo(Codec<T> codec, HolderLookup.Provider registries, CompoundTag destination, String key, @Nullable T value) {
        if (value == null) return;
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
            .resultOrPartial(e -> LOG.warn("Failed to save NBT: {}", e))
            .ifPresent(x -> destination.put(key, x));
    }

    public static @Nullable Object toLua(@Nullable Tag tag) {
        if (tag == null) return null;

        return switch (tag.getId()) {
            case Tag.TAG_BYTE, Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG -> ((NumericTag) tag).getAsLong();
            case Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> ((NumericTag) tag).getAsDouble();
            case Tag.TAG_STRING -> tag.getAsString();
            case Tag.TAG_COMPOUND -> {
                var compound = (CompoundTag) tag;
                Map<String, Object> map = new HashMap<>(compound.size());
                for (var key : compound.getAllKeys()) {
                    var value = toLua(compound.get(key));
                    if (value != null) map.put(key, value);
                }
                yield map;
            }
            case Tag.TAG_LIST -> ((ListTag) tag).stream().map(NBTUtil::toLua).toList();
            case Tag.TAG_BYTE_ARRAY -> {
                var array = ((ByteArrayTag) tag).getAsByteArray();
                List<Byte> map = new ArrayList<>(array.length);
                for (var b : array) map.add(b);
                yield map;
            }
            case Tag.TAG_INT_ARRAY -> Arrays.stream(((IntArrayTag) tag).getAsIntArray()).boxed().toList();
            case Tag.TAG_LONG_ARRAY -> Arrays.stream(((LongArrayTag) tag).getAsLongArray()).boxed().toList();
            default -> null;
        };
    }

    @Nullable
    public static String getNBTHash(@Nullable Tag tag) {
        if (tag == null) return null;

        try {
            var digest = MessageDigest.getInstance("MD5");
            DataOutput output = new DataOutputStream(new DigestOutputStream(digest));
            writeNamedTag(output, "", tag);
            var hash = digest.digest();
            return ENCODING.encode(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            LOG.error("Cannot hash NBT", e);
            return null;
        }
    }

    /**
     * An alternative version of {@link NbtIo#write(CompoundTag, DataOutput)}, which sorts keys. This
     * should make the output slightly more deterministic.
     *
     * @param output The output to write to.
     * @param name   The name of the key we're writing. Should be {@code ""} for the root node.
     * @param tag    The tag to write.
     * @throws IOException If the underlying stream throws.
     * @see NbtIo#write(CompoundTag, DataOutput)
     * @see CompoundTag#write(DataOutput)
     * @see ListTag#write(DataOutput)
     */
    private static void writeNamedTag(DataOutput output, String name, Tag tag) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() == 0) return;
        output.writeUTF(name);
        writeTag(output, tag);
    }

    private static void writeTag(DataOutput output, Tag tag) throws IOException {
        if (tag instanceof CompoundTag compound) {
            var keys = compound.getAllKeys().toArray(new String[0]);
            Arrays.sort(keys);
            for (var key : keys) writeNamedTag(output, key, Nullability.assertNonNull(compound.get(key)));

            output.writeByte(0);
        } else if (tag instanceof ListTag list) {
            output.writeByte(list.isEmpty() ? 0 : list.get(0).getId());
            output.writeInt(list.size());
            for (var value : list) writeTag(output, value);
        } else {
            tag.write(output);
        }
    }

    @VisibleForTesting
    static final class DigestOutputStream extends OutputStream {
        private final MessageDigest digest;

        DigestOutputStream(MessageDigest digest) {
            this.digest = digest;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            digest.update(b, off, len);
        }

        @Override
        public void write(int b) {
            digest.update((byte) b);
        }
    }
}
