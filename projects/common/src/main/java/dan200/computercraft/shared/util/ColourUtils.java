/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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

    public static @Nullable DyeColor getStackColour(ItemStack stack) {
        if (stack.isEmpty()) return null;

        for (var i = 0; i < DYES.size(); i++) {
            var dye = DYES.get(i);
            if (stack.is(dye)) return DyeColor.byId(i);
        }

        return null;
    }
}
