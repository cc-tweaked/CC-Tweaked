// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.platform.PlatformHelper;
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

    private static final CompoundTag EMPTY_TAG;

    static {
        // If in a development environment, create a magic immutable compound tag.
        // We avoid doing this in prod, as I fear it might mess up the JIT inlining things.
        if (PlatformHelper.get().isDevelopmentEnvironment()) {
            try {
                var ctor = CompoundTag.class.getDeclaredConstructor(Map.class);
                ctor.setAccessible(true);
                EMPTY_TAG = ctor.newInstance(Map.of());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            EMPTY_TAG = new CompoundTag();
        }
    }

    private NBTUtil() {
    }

    /**
     * Get a singleton empty {@link CompoundTag}. This tag should never be modified.
     *
     * @return The empty compound tag.
     */
    public static CompoundTag emptyTag() {
        if (EMPTY_TAG.size() != 0) LOG.error("The empty tag has been modified.");
        return EMPTY_TAG;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        var childTag = tag.get(key);
        return childTag != null && childTag.getId() == Tag.TAG_COMPOUND ? (CompoundTag) childTag : emptyTag();
    }

    public static @Nullable Object toLua(@Nullable Tag tag) {
        if (tag == null) return null;

        return switch (tag.getId()) {
            case Tag.TAG_BYTE, Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG -> ((NumericTag) tag).getAsLong();
            case Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> ((NumericTag) tag).getAsDouble();
            case Tag.TAG_STRING -> tag.getAsString();
            case Tag.TAG_COMPOUND -> toLua((CompoundTag) tag);
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

    public static Map<String, Object> toLua(CompoundTag tag) {
        Map<String, Object> map = new HashMap<>(tag.size());
        for (var key : tag.getAllKeys()) {
            var value = toLua(tag.get(key));
            if (value != null) map.put(key, value);
        }
        return map;
    }

    @Nullable
    public static String getNBTHash(@Nullable CompoundTag tag) {
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
