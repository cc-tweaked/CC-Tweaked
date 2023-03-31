// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public final class ColourUtils {
    private static final List<TagKey<Item>> DYES = PlatformHelper.get().getDyeTags();

    private ColourUtils() {
    }

    public static TagKey<Item> getDyeTag(DyeColor color) {
        return DYES.get(color.getId());
    }

    public static @Nullable DyeColor getStackColour(ItemStack stack) {
        if (stack.isEmpty()) return null;

        for (var i = 0; i < DYES.size(); i++) {
            var dye = DYES.get(i);
            if (stack.is(dye)) return DyeColor.byId(i);
        }

        return null;
    }
}
