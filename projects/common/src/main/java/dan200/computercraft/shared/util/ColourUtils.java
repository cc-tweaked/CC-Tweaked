// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ColorCollection;
import org.jspecify.annotations.Nullable;

public final class ColourUtils {
    private static final ColorCollection<TagKey<Item>> DYES = PlatformHelper.get().getDyeTags();

    private ColourUtils() {
    }

    public static TagKey<Item> getDyeTag(DyeColor color) {
        return DYES.pick(color);
    }

    public static boolean isDye(ItemStack stack) {
        return stack.is(ItemTags.DYES);
    }

    public static @Nullable DyeColor getDyeColour(ItemStack stack) {
        return isDye(stack) ? stack.get(DataComponents.DYE) : null;
    }
}
