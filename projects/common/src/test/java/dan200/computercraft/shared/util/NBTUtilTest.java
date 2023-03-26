// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.test.shared.WithMinecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.shared.util.NBTUtil.getNBTHash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private static CompoundTag makeCompoundTag(boolean reverse) {
        var nbt = new CompoundTag();

        if (reverse) {
            nbt.putString("c", "c");
            nbt.putString("b", "b");
            nbt.putString("a", "a");
        } else {
            nbt.putString("a", "a");
            nbt.putString("b", "b");
            nbt.putString("c", "c");
        }

        return nbt;
    }

    private static ListTag makeListTag(boolean reverse) {
        var list = new ListTag();

        for (int i = 0; i < 3; i++) {
            list.add(makeCompoundTag(reverse));
        }

        return list;
    }
}
