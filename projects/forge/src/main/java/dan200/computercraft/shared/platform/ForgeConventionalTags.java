// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * {@code ConventionalItemTags} for Forge for cases where there's no existing one.
 */
public class ForgeConventionalTags {
    public static final TagKey<Item> SHULKER_BOXES = item("shulker_boxes");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", name));
    }
}
