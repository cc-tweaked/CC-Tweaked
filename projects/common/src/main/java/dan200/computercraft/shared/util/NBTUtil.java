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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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

    private static @Nullable Tag toNBTTag(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Boolean) return ByteTag.valueOf((byte) ((boolean) (Boolean) object ? 1 : 0));
        if (object instanceof Number) return DoubleTag.valueOf(((Number) object).doubleValue());
        if (object instanceof String) return StringTag.valueOf(object.toString());
        if (object instanceof Map<?, ?> m) {
            var nbt = new CompoundTag();
            var i = 0;
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                var key = toNBTTag(entry.getKey());
                var value = toNBTTag(entry.getKey());
                if (key != null && value != null) {
                    nbt.put("k" + i, key);
                    nbt.put("v" + i, value);
                    i++;
                }
            }
            nbt.putInt("len", m.size());
            return nbt;
        }

        return null;
    }

    public static @Nullable CompoundTag encodeObjects(@Nullable Object[] objects) {
        if (objects == null || objects.length == 0) return null;

        var nbt = new CompoundTag();
        nbt.putInt("len", objects.length);
        for (var i = 0; i < objects.length; i++) {
            var child = toNBTTag(objects[i]);
            if (child != null) nbt.put(Integer.toString(i), child);
        }
        return nbt;
    }

    private static @Nullable Object fromNBTTag(@Nullable Tag tag) {
        if (tag == null) return null;
        switch (tag.getId()) {
            case Tag.TAG_BYTE:
                return ((ByteTag) tag).getAsByte() > 0;
            case Tag.TAG_DOUBLE:
                return ((DoubleTag) tag).getAsDouble();
            default:
            case Tag.TAG_STRING:
                return tag.getAsString();
            case Tag.TAG_COMPOUND: {
                var c = (CompoundTag) tag;
                var len = c.getInt("len");
                Map<Object, Object> map = new HashMap<>(len);
                for (var i = 0; i < len; i++) {
                    var key = fromNBTTag(c.get("k" + i));
                    var value = fromNBTTag(c.get("v" + i));
                    if (key != null && value != null) map.put(key, value);
                }
                return map;
            }
        }
    }

    public static @Nullable Object toLua(@Nullable Tag tag) {
        if (tag == null) return null;

        switch (tag.getId()) {
            case Tag.TAG_BYTE:
            case Tag.TAG_SHORT:
            case Tag.TAG_INT:
            case Tag.TAG_LONG:
                return ((NumericTag) tag).getAsLong();
            case Tag.TAG_FLOAT:
            case Tag.TAG_DOUBLE:
                return ((NumericTag) tag).getAsDouble();
            case Tag.TAG_STRING: // String
                return tag.getAsString();
            case Tag.TAG_COMPOUND: { // Compound
                var compound = (CompoundTag) tag;
                Map<String, Object> map = new HashMap<>(compound.size());
                for (var key : compound.getAllKeys()) {
                    var value = toLua(compound.get(key));
                    if (value != null) map.put(key, value);
                }
                return map;
            }
            case Tag.TAG_LIST: {
                var list = (ListTag) tag;
                List<Object> map = new ArrayList<>(list.size());
                for (var value : list) map.add(toLua(value));
                return map;
            }
            case Tag.TAG_BYTE_ARRAY: {
                var array = ((ByteArrayTag) tag).getAsByteArray();
                List<Byte> map = new ArrayList<>(array.length);
                for (var b : array) map.add(b);
                return map;
            }
            case Tag.TAG_INT_ARRAY: {
                var array = ((IntArrayTag) tag).getAsIntArray();
                List<Integer> map = new ArrayList<>(array.length);
                for (var j : array) map.add(j);
                return map;
            }

            default:
                return null;
        }
    }

    public static @Nullable Object[] decodeObjects(CompoundTag tag) {
        var len = tag.getInt("len");
        if (len <= 0) return null;

        var objects = new Object[len];
        for (var i = 0; i < len; i++) {
            var key = Integer.toString(i);
            if (tag.contains(key)) {
                objects[i] = fromNBTTag(tag.get(key));
            }
        }
        return objects;
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
