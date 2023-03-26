// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.test.shared.WithMinecraft;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static dan200.computercraft.shared.util.NBTUtil.getNBTHash;
import static org.junit.jupiter.api.Assertions.*;

@WithMinecraft
public class NBTUtilTest {
    @Test
    public void testCompoundTagSorting() {
        var nbt1 = makeCompoundTag(false);
        var hash1 = getNBTHash(nbt1);

        var nbt2 = makeCompoundTag(true);
        var hash2 = getNBTHash(nbt2);

        assertNotNull(hash1, "NBT hash should not be null");
        assertNotNull(hash2, "NBT hash should not be null");
        assertEquals(hash1, hash2, "NBT hashes should be equal");
    }

    @Test
    public void testListTagSorting() {
        var nbt1 = new CompoundTag();
        nbt1.put("Items", makeListTag(false));
        var hash1 = getNBTHash(nbt1);

        var nbt2 = new CompoundTag();
        nbt2.put("Items", makeListTag(true));
        var hash2 = getNBTHash(nbt2);

        assertNotNull(hash1, "NBT hash should not be null");
        assertNotNull(hash2, "NBT hash should not be null");
        assertEquals(hash1, hash2, "NBT hashes should be equal");
    }

    @Test
    public void testTagsHaveDifferentOrders() {
        var nbt1 = makeCompoundTag(false);
        var nbt2 = makeCompoundTag(true);
        assertNotEquals(
            List.copyOf(nbt1.getAllKeys()), List.copyOf(nbt2.getAllKeys()),
            "Expected makeCompoundTag to return keys with different orders."
        );
    }

    @Test
    public void testHashEquivalentForBasicValues() {
        var nbt = new CompoundTag();
        nbt.put("list", Util.make(new ListTag(), list -> {
            list.add(Util.make(new CompoundTag(), t -> t.putBoolean("key", true)));
            list.add(Util.make(new CompoundTag(), t -> t.putInt("key", 23)));
        }));

        assertEquals(getNBTHash(nbt), getNBTHashDefault(nbt));
    }

    private static CompoundTag makeCompoundTag(boolean grow) {
        var nbt = new CompoundTag();

        nbt.putString("Slot", "Slot 1");
        nbt.putString("Count", "64");
        nbt.putString("id", "123");

        // Grow the map to cause a rehash and (hopefully) change the order around a little bit.
        if (grow) {
            for (var i = 0; i < 64; i++) nbt.putBoolean("x" + i, true);
            for (var i = 0; i < 64; i++) nbt.remove("x" + i);
        }

        return nbt;
    }

    private static ListTag makeListTag(boolean reverse) {
        var list = new ListTag();

        for (var i = 0; i < 3; i++) {
            list.add(makeCompoundTag(reverse));
        }

        return list;
    }

    /**
     * Equivalent to {@link NBTUtil#getNBTHash(CompoundTag)}, but using the default {@link NbtIo#write(CompoundTag, File)} method.
     *
     * @param tag The tag to hash.
     * @return The resulting hash.
     */
    private static String getNBTHashDefault(CompoundTag tag) {
        try {
            var digest = MessageDigest.getInstance("MD5");
            DataOutput output = new DataOutputStream(new NBTUtil.DigestOutputStream(digest));
            NbtIo.write(tag, output);
            var hash = digest.digest();
            return NBTUtil.ENCODING.encode(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
