/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import org.apache.commons.codec.binary.Hex;

import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class NBTUtil {
    public static final int TAG_END = 0;
    public static final int TAG_BYTE = 1;
    public static final int TAG_SHORT = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_BYTE_ARRAY = 7;
    public static final int TAG_STRING = 8;
    public static final int TAG_LIST = 9;
    public static final int TAG_COMPOUND = 10;
    public static final int TAG_INT_ARRAY = 11;
    public static final int TAG_LONG_ARRAY = 12;
    public static final int TAG_ANY_NUMERIC = 99;

    private NBTUtil() {}

    private static Tag toNBTTag(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Boolean) {
            return ByteTag.of((byte) ((boolean) (Boolean) object ? 1 : 0));
        }
        if (object instanceof Number) {
            return DoubleTag.of(((Number) object).doubleValue());
        }
        if (object instanceof String) {
            return StringTag.of(object.toString());
        }
        if (object instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) object;
            CompoundTag nbt = new CompoundTag();
            int i = 0;
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                Tag key = toNBTTag(entry.getKey());
                Tag value = toNBTTag(entry.getKey());
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

    public static CompoundTag encodeObjects(Object[] objects) {
        if (objects == null || objects.length <= 0) {
            return null;
        }

        CompoundTag nbt = new CompoundTag();
        nbt.putInt("len", objects.length);
        for (int i = 0; i < objects.length; i++) {
            Tag child = toNBTTag(objects[i]);
            if (child != null) {
                nbt.put(Integer.toString(i), child);
            }
        }
        return nbt;
    }

    private static Object fromNBTTag(Tag tag) {
        if (tag == null) {
            return null;
        }
        switch (tag.getType()) {
        case TAG_BYTE:
            return ((ByteTag) tag).getByte() > 0;
        case TAG_DOUBLE:
            return ((DoubleTag) tag).getDouble();
        default:
        case TAG_STRING:
            return tag.asString();
        case TAG_COMPOUND: {
            CompoundTag c = (CompoundTag) tag;
            int len = c.getInt("len");
            Map<Object, Object> map = new HashMap<>(len);
            for (int i = 0; i < len; i++) {
                Object key = fromNBTTag(c.get("k" + i));
                Object value = fromNBTTag(c.get("v" + i));
                if (key != null && value != null) {
                    map.put(key, value);
                }
            }
            return map;
        }
        }
    }

    public static Object toLua(Tag tag) {
        if (tag == null) {
            return null;
        }

        byte typeID = tag.getType();
        switch (typeID) {
        case TAG_BYTE:
        case TAG_SHORT:
        case TAG_INT:
        case TAG_LONG:
            return ((AbstractNumberTag) tag).getLong();
        case TAG_FLOAT:
        case TAG_DOUBLE:
            return ((AbstractNumberTag) tag).getDouble();
        case TAG_STRING: // String
            return tag.asString();
        case TAG_COMPOUND: // Compound
        {
            CompoundTag compound = (CompoundTag) tag;
            Map<String, Object> map = new HashMap<>(compound.getSize());
            for (String key : compound.getKeys()) {
                Object value = toLua(compound.get(key));
                if (value != null) {
                    map.put(key, value);
                }
            }
            return map;
        }
        case TAG_LIST: {
            ListTag list = (ListTag) tag;
            Map<Integer, Object> map = new HashMap<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                map.put(i, toLua(list.get(i)));
            }
            return map;
        }
        case TAG_BYTE_ARRAY: {
            byte[] array = ((ByteArrayTag) tag).getByteArray();
            Map<Integer, Byte> map = new HashMap<>(array.length);
            for (int i = 0; i < array.length; i++) {
                map.put(i + 1, array[i]);
            }
            return map;
        }
        case TAG_INT_ARRAY: {
            int[] array = ((IntArrayTag) tag).getIntArray();
            Map<Integer, Integer> map = new HashMap<>(array.length);
            for (int i = 0; i < array.length; i++) {
                map.put(i + 1, array[i]);
            }
            return map;
        }

        default:
            return null;
        }
    }

    public static Object[] decodeObjects(CompoundTag tag) {
        int len = tag.getInt("len");
        if (len <= 0) {
            return null;
        }

        Object[] objects = new Object[len];
        for (int i = 0; i < len; i++) {
            String key = Integer.toString(i);
            if (tag.contains(key)) {
                objects[i] = fromNBTTag(tag.get(key));
            }
        }
        return objects;
    }

    @Nullable
    public static String getNBTHash(@Nullable CompoundTag tag) {
        if (tag == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            DataOutput output = new DataOutputStream(new DigestOutputStream(digest));
            NbtIo.write(tag, output);
            byte[] hash = digest.digest();
            return new String(Hex.encodeHex(hash));
        } catch (NoSuchAlgorithmException | IOException e) {
            ComputerCraft.log.error("Cannot hash NBT", e);
            return null;
        }
    }

    private static final class DigestOutputStream extends OutputStream {
        private final MessageDigest digest;

        DigestOutputStream(MessageDigest digest) {
            this.digest = digest;
        }

        @Override
        public void write(int b) {
            this.digest.update((byte) b);
        }

        @Override
        public void write(@Nonnull byte[] b, int off, int len) {
            this.digest.update(b, off, len);
        }
    }
}
